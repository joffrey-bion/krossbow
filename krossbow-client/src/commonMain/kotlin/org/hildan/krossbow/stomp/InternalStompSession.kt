package org.hildan.krossbow.stomp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.frame.StompParser
import org.hildan.krossbow.stomp.frame.encodeToBytes
import org.hildan.krossbow.stomp.frame.encodeToText
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.stomp.headers.StompDisconnectHeaders
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.stomp.headers.StompUnsubscribeHeaders
import org.hildan.krossbow.utils.SuspendingAtomicInt
import org.hildan.krossbow.utils.getStringAndInc
import org.hildan.krossbow.websocket.KWebSocketListener
import org.hildan.krossbow.websocket.KWebSocketSession
import kotlin.reflect.KClass

/**
 * A coroutine-based STOMP session API.
 */
@UseExperimental(ExperimentalCoroutinesApi::class) // for broadcast channel
internal class InternalStompSession(
    private val config: StompConfig,
    private val webSocketSession: KWebSocketSession
) : StompSession, KWebSocketListener {

    private val nextSubscriptionId = SuspendingAtomicInt(0)

    private val nextReceiptId = SuspendingAtomicInt(0)

    private val subscriptionsById: MutableMap<String, Subscription<*>> = mutableMapOf()

    private val nonMsgFrames = BroadcastChannel<StompFrame>(Channel.BUFFERED)

    init {
        webSocketSession.listener = this
    }

    override suspend fun onTextMessage(text: String) = onFrameReceived(StompParser.parse(text))

    override suspend fun onBinaryMessage(bytes: ByteArray) = onFrameReceived(StompParser.parse(bytes))

    override suspend fun onError(error: Throwable) {
        // TODO allow user to set a global onError listener?
        throw IllegalStateException("Error at WebSocket level: ${error.message}", error)
    }

    override suspend fun onClose() {
        // TODO allow user to set a global onClose listener?
        println("Underlying WebSocket connection closed")
    }

    private suspend fun onFrameReceived(frame: StompFrame) {
        when (frame) {
            is StompFrame.Message -> onMessageFrameReceived(frame)
            // TODO handle global error frames without duplicating the ones associated with a subscription/receipt
            else -> nonMsgFrames.send(frame)
        }
    }

    private suspend fun onMessageFrameReceived(frame: StompFrame.Message) {
        subscriptionsById.getValue(frame.headers.subscription).onMessage(frame)
    }

    internal suspend fun connect(headers: StompConnectHeaders): StompFrame.Connected = coroutineScope {
        val connectedFrame = async {
            waitForTypedFrame<StompFrame.Connected>()
        }
        sendStompFrameAsIs(StompFrame.Connect(headers))
        connectedFrame.await()
        // TODO initialize heart beat coroutine if server is ok with it
    }

    private suspend inline fun <reified T : StompFrame> waitForTypedFrame(predicate: (T) -> Boolean = { true }): T {
        val frameSubscription = nonMsgFrames.openSubscription()
        try {
            for (f in frameSubscription) {
                if (f is StompFrame.Error) {
                    throw StompErrorFrameReceived(f)
                }
                if (f is T && predicate(f)) {
                    return f
                }
            }
        } finally {
            frameSubscription.cancel()
        }
        throw IllegalStateException("Connection closed unexpectedly while expecting frame of type ${T::class}")
    }

    override suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt? {
        if (headers.contentLength == null) {
            headers.contentLength = body?.bytes?.size ?: 0
        }
        return sendStompFrame(StompFrame.Send(headers, body))
    }

    override suspend fun <T : Any> send(
        headers: StompSendHeaders,
        payload: T?,
        payloadType: KClass<T>
    ): StompReceipt? {
        val frameContent = config.messageConverter.serialize(payload, payloadType)
        headers.contentType = headers.contentType ?: frameContent.contentType
        headers.contentLength = headers.contentLength ?: frameContent.contentLength
        headers.putAll(frameContent.customHeaders)
        return send(headers, frameContent.body)
    }

    private suspend fun sendStompFrame(frame: StompFrame): StompReceipt? {
        val receiptId = getReceiptAndMaybeSetAuto(frame)
        if (receiptId == null) {
            sendStompFrameAsIs(frame)
            return null
        }
        sendAndWaitForReceipt(receiptId, frame)
        return StompReceipt(receiptId)
    }

    private suspend fun getReceiptAndMaybeSetAuto(frame: StompFrame): String? {
        if (config.autoReceipt) {
            if (frame.headers.receipt == null) {
                frame.headers.receipt = nextReceiptId.getStringAndInc()
            }
        }
        return frame.headers.receipt
    }

    private suspend fun sendStompFrameAsIs(frame: StompFrame) {
        if (frame.body is FrameBody.Binary) {
            webSocketSession.sendBinary(frame.encodeToBytes())
        } else {
            // frames without payloads are also sent as text because the headers are always textual
            // Also, some sockJS implementations don't support binary frames
            webSocketSession.sendText(frame.encodeToText())
        }
    }

    private suspend fun sendAndWaitForReceipt(receiptId: String, frame: StompFrame) {
        coroutineScope {
            val receiptFrame = async { waitForReceipt(receiptId) }
            sendStompFrameAsIs(frame)
            try {
                withTimeout(config.receiptTimeLimit) { receiptFrame.await() }
            } catch (e: TimeoutCancellationException) {
                throw LostReceiptException(receiptId)
            }
        }
    }

    private suspend fun waitForReceipt(receiptId: String): StompFrame.Receipt {
        return waitForTypedFrame { it.headers.receiptId == receiptId }
    }

    override suspend fun <T : Any> subscribe(destination: String, clazz: KClass<T>): StompSubscription<T> =
            subscribe(destination) { config.messageConverter.deserialize(it, clazz) }

    override suspend fun subscribeNoPayload(destination: String): StompSubscription<Unit> =
            subscribe(destination) { StompMessage(Unit, it.headers) }

    private suspend fun <T> subscribe(
        destination: String,
        convertPayload: (StompFrame.Message) -> StompMessage<T>
    ): StompSubscription<T> {
        val id = nextSubscriptionId.getAndIncrement().toString()
        val sub = Subscription(id, convertPayload, this)
        subscriptionsById[id] = sub
        val subscribeFrame = StompFrame.Subscribe(StompSubscribeHeaders(destination = destination, id = id))
        sendStompFrame(subscribeFrame)
        return sub
    }

    internal suspend fun unsubscribe(subscriptionId: String) {
        sendStompFrameAsIs(StompFrame.Unsubscribe(StompUnsubscribeHeaders(id = subscriptionId)))
        subscriptionsById.remove(subscriptionId)
    }

    override suspend fun disconnect() {
        if (config.gracefulDisconnect) {
            val frame = StompFrame.Disconnect(StompDisconnectHeaders(nextReceiptId.getStringAndInc()))
            // TODO reduce timeout and allow lost receipt for disconnect (see connection lingering in spec)
            sendStompFrame(frame)
        }
        nonMsgFrames.cancel()
        webSocketSession.close()
    }
}

private class Subscription<out T>(
    override val id: String,
    private val convertPayload: (StompFrame.Message) -> StompMessage<T>,
    private val internalSession: InternalStompSession
) : StompSubscription<T> {

    private val internalMsgChannel: Channel<StompMessage<T>> = Channel()

    override val messages: ReceiveChannel<StompMessage<T>> get() = internalMsgChannel

    suspend fun onMessage(message: StompFrame.Message) {
        internalMsgChannel.send(convertPayload(message))
    }

    fun onError(error: StompFrame.Error) {
        internalMsgChannel.close(StompErrorFrameReceived(error))
    }

    override suspend fun unsubscribe() {
        internalSession.unsubscribe(id)
        internalMsgChannel.close()
    }
}
