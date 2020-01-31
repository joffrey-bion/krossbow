package org.hildan.krossbow.engines.mpp

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.KrossbowEngineClient
import org.hildan.krossbow.engines.KrossbowEngineConfig
import org.hildan.krossbow.engines.KrossbowEngineSession
import org.hildan.krossbow.engines.KrossbowEngineSubscription
import org.hildan.krossbow.engines.KrossbowMessage
import org.hildan.krossbow.engines.KrossbowReceipt
import org.hildan.krossbow.engines.SubscriptionCallbacks
import org.hildan.krossbow.engines.mpp.frame.FrameBody
import org.hildan.krossbow.engines.mpp.frame.StompFrame
import org.hildan.krossbow.engines.mpp.frame.StompParser
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys
import org.hildan.krossbow.engines.mpp.headers.StompConnectHeaders
import org.hildan.krossbow.engines.mpp.headers.StompDisconnectHeaders
import org.hildan.krossbow.engines.mpp.headers.StompSendHeaders
import org.hildan.krossbow.engines.mpp.headers.StompSubscribeHeaders
import org.hildan.krossbow.engines.mpp.headers.StompUnsubscribeHeaders
import org.hildan.krossbow.engines.mpp.websocket.KtorWebSocket
import org.hildan.krossbow.engines.mpp.websocket.WebSocket
import org.hildan.krossbow.engines.mpp.websocket.WebSocketSession

object MppKrossbowEngine: KrossbowEngine {

    override fun createClient(config: KrossbowEngineConfig): KrossbowEngineClient =
            MppKrossbowEngineClient(config, KtorWebSocket())
}

@UseExperimental(KtorExperimentalAPI::class)
class MppKrossbowEngineClient(
    private val config: KrossbowEngineConfig,
    private val webSocket: WebSocket
): KrossbowEngineClient {

    override suspend fun connect(url: String, login: String?, passcode: String?): KrossbowEngineSession =
            MppKrossbowEngineSession(config, webSocket.connect(url)).apply {
                // connect at STOMP protocol level
                connect(url, login, passcode)
            }
}

class MppKrossbowEngineSession(
    private val config: KrossbowEngineConfig,
    private val webSocketSession: WebSocketSession
): KrossbowEngineSession {

    private var nextSubscriptionId = SuspendingAtomicInt(0)

    private var nextReceiptId = SuspendingAtomicInt(0)

    private val subscriptionsById: MutableMap<String, SubscriptionCallbacks<ByteArray>> = mutableMapOf()

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private val nonMsgFrames = BroadcastChannel<StompFrame>(Channel.Factory.BUFFERED)

    // TODO maybe change the WS API to use a listener (with callbacks) instead of a channel to avoid this coroutine
    private val wsListenerJob = GlobalScope.launch {
        for (frameBytes in webSocketSession.incomingFrames) {
            onFrameReceived(frameBytes)
        }
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private suspend fun onFrameReceived(frameBytes: ByteArray) {
        when (val frame = StompParser.parse(frameBytes)) {
            is StompFrame.Message -> onMessageFrameReceived(frame)
            else -> nonMsgFrames.send(frame)
        }
    }

    private suspend fun onMessageFrameReceived(frame: StompFrame.Message) {
        val payload = frame.body?.rawBytes ?: ByteArray(0)
        subscriptionsById.getValue(frame.headers.subscription).onReceive(KrossbowMessage(payload, frame.headers))
    }

    suspend fun connect(url: String, login: String?, passcode: String?): StompFrame.Connected {
        val host = url.substringAfter("://").substringBefore("/").substringBefore(":")
        val connectFrame = StompFrame.Connect(StompConnectHeaders(host = host, login = login, passcode = passcode))
        return coroutineScope {
            val connectedFrame = async { waitForTypedFrame<StompFrame.Connected>() }
            sendStompFrameWithoutReceipt(connectFrame)
            connectedFrame.await()
        }
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private suspend inline fun <reified T : StompFrame> waitForTypedFrame(predicate: (T) -> Boolean = { true }): T {
        val frameSubscription = nonMsgFrames.openSubscription()
        for (f in frameSubscription) {
            if (f is StompFrame.Error) {
                frameSubscription.cancel()
                throw StompErrorFrameReceived(f)
            }
            if (f is T && predicate(f)) {
                frameSubscription.cancel()
                return f
            }
        }
        throw IllegalStateException("Connection closed unexpectedly while expecting frame of type ${T::class}")
    }

    private suspend fun waitForReceipt(receiptId: String): StompFrame.Receipt {
        return waitForTypedFrame { it.headers.receiptId == receiptId }
    }

    @UseExperimental(ExperimentalStdlibApi::class)
    override suspend fun send(destination: String, body: ByteArray?): KrossbowReceipt? {
        val sendFrame = StompFrame.Send(StompSendHeaders(destination), body?.let { FrameBody.Binary(it) })
        return sendStompFrame(sendFrame)
    }

    override suspend fun subscribe(destination: String, callbacks: SubscriptionCallbacks<ByteArray>): KrossbowEngineSubscription {
        val id = nextSubscriptionId.getAndIncrement().toString()
        subscriptionsById[id] = callbacks
        val subscribeFrame = StompFrame.Subscribe(StompSubscribeHeaders(destination = destination, id = id))
        sendStompFrame(subscribeFrame)
        return KrossbowEngineSubscription(id) {
            sendStompFrameWithoutReceipt(StompFrame.Unsubscribe(StompUnsubscribeHeaders(id = id)))
            subscriptionsById.remove(id)
        }
    }

    override suspend fun subscribeNoPayload(destination: String, callbacks: SubscriptionCallbacks<Unit>): KrossbowEngineSubscription {
        return subscribe(destination, object : SubscriptionCallbacks<ByteArray> {
            override suspend fun onReceive(message: KrossbowMessage<ByteArray>) =
                    callbacks.onReceive(KrossbowMessage(Unit, message.headers))

            override fun onError(throwable: Throwable) = callbacks.onError(throwable)
        })
    }

    private suspend fun sendStompFrame(sendFrame: StompFrame): KrossbowReceipt? {
        return if (config.autoReceipt) {
            val receiptFrame = sendStompFrameWithReceipt(sendFrame)
            KrossbowReceipt(receiptFrame.headers.receiptId)
        } else {
            sendStompFrameWithoutReceipt(sendFrame)
            null
        }
    }

    private suspend fun sendStompFrameWithoutReceipt(frame: StompFrame) {
        webSocketSession.send(frame.toBytes())
    }

    private suspend fun sendStompFrameWithReceipt(frame: StompFrame): StompFrame.Receipt {
        // FIXME actually set the receipt header on the frame to send
        val receiptId = frame.headers.getValue(HeaderKeys.RECEIPT_ID) ?: nextReceiptId.getAndIncrement().toString()
        return coroutineScope {
            val receiptFrame = async { waitForReceipt(receiptId) }
            webSocketSession.send(frame.toBytes())
            receiptFrame.await()
        }
    }

    override suspend fun disconnect() {
        // TODO maybe give a config option for auto-receipt on disconnect only
        sendStompFrame(StompFrame.Disconnect(StompDisconnectHeaders()))
        wsListenerJob.cancelAndJoin()
        webSocketSession.close()
    }
}

class SuspendingAtomicInt(private var value: Int = 0) {

    private val mutex = Mutex()

    // TODO maybe change this to actor to completely avoid locking
    // https://kotlinlang.org/docs/reference/coroutines/shared-mutable-state-and-concurrency.html#actors
    suspend fun getAndIncrement(): Int = mutex.withLock { value++ }
}

class StompErrorFrameReceived(val frame: StompFrame.Error): Exception("STOMP ERROR frame received: ${frame.message}")
