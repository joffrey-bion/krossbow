package org.hildan.krossbow.stomp.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import org.hildan.krossbow.converters.MessageConverter
import org.hildan.krossbow.converters.StringMessageConverter
import org.hildan.krossbow.stomp.KrossbowEngineSubscription
import org.hildan.krossbow.stomp.KrossbowMessage
import org.hildan.krossbow.stomp.KrossbowReceipt
import org.hildan.krossbow.stomp.KrossbowSubscription
import org.hildan.krossbow.stomp.SubscriptionCallbacks
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.frame.StompParser
import org.hildan.krossbow.stomp.frame.encodeToBytes
import org.hildan.krossbow.stomp.frame.encodeToText
import org.hildan.krossbow.stomp.headers.HeaderKeys
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.stomp.headers.StompDisconnectHeaders
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.stomp.headers.StompUnsubscribeHeaders
import org.hildan.krossbow.stomp.headers.ensureReceiptHeader
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
    private val config: StompConfig, private val webSocketSession: KWebSocketSession
) : StompSession, KWebSocketListener {

    private val nextSubscriptionId = SuspendingAtomicInt(0)

    private val nextReceiptId = SuspendingAtomicInt(0)

    private val subscriptionsById: MutableMap<String, SubscriptionCallbacks<ByteArray>> = mutableMapOf()

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
        val payload = frame.body?.rawBytes ?: ByteArray(0)
        val message = KrossbowMessage(payload, frame.headers)
        subscriptionsById.getValue(frame.headers.subscription).onReceive(message)
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

    override suspend fun send(headers: StompSendHeaders, body: FrameBody?): KrossbowReceipt? {
        return sendStompFrame(StompFrame.Send(headers, body))
    }

    override suspend fun <T : Any> send(
        headers: StompSendHeaders,
        payload: T?,
        payloadType: KClass<T>
    ): KrossbowReceipt? {
        val bytesBody = convertFrameBody(payload, payloadType)
        return send(headers, bytesBody)
    }

    private fun <T : Any> convertFrameBody(payload: T?, payloadType: KClass<T>): FrameBody? {
        if (payload == null) {
            return null
        }
        return when (val converter = config.messageConverter) {
            is StringMessageConverter -> FrameBody.Text(converter.convertToString(payload, payloadType))
            else -> FrameBody.Binary(converter.convertToBytes(payload, payloadType))
        }
    }

    private suspend fun sendStompFrame(frame: StompFrame): KrossbowReceipt? {
        if (config.autoReceipt) {
            frame.headers.ensureReceiptHeader { nextReceiptId.getStringAndInc() }
        }
        val receiptId = frame.headers[HeaderKeys.RECEIPT]
        if (receiptId == null) {
            sendStompFrameAsIs(frame)
            return null
        }
        sendAndWaitForReceipt(receiptId, frame)
        return KrossbowReceipt(receiptId)
    }

    private suspend fun sendAndWaitForReceipt(receiptId: String, frame: StompFrame) {
        coroutineScope {
            val receiptFrame = async { waitForReceipt(receiptId) }
            sendStompFrameAsIs(frame)
            receiptFrame.await()
        }
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

    private suspend fun waitForReceipt(receiptId: String): StompFrame.Receipt {
        return waitForTypedFrame { it.headers.receiptId == receiptId }
    }

    override suspend fun <T : Any> subscribe(destination: String, clazz: KClass<T>): KrossbowSubscription<T> {
        val converter = config.messageConverter
        val channel = Channel<KrossbowMessage<T>>()
        val callbacks = CallbacksAdapter(converter, clazz, channel)
        val sub = subscribe(destination, callbacks)
        return KrossbowSubscription(sub.id, sub.unsubscribe, channel)
    }

    override suspend fun subscribeNoPayload(destination: String): KrossbowSubscription<Unit> {
        val channel = Channel<KrossbowMessage<Unit>>()
        val sub = subscribe(destination, object : SubscriptionCallbacks<ByteArray> {
            override suspend fun onReceive(message: KrossbowMessage<ByteArray>) =
                    channel.send(KrossbowMessage(Unit, message.headers))

            override fun onError(throwable: Throwable) {
                channel.close(throwable)
            }
        })
        return KrossbowSubscription(sub.id, sub.unsubscribe, channel)
    }

    private suspend fun subscribe(destination: String, callbacks: SubscriptionCallbacks<ByteArray>): KrossbowEngineSubscription {
        val id = nextSubscriptionId.getAndIncrement().toString()
        subscriptionsById[id] = callbacks
        val subscribeFrame = StompFrame.Subscribe(StompSubscribeHeaders(destination = destination, id = id))
        sendStompFrame(subscribeFrame)
        return KrossbowEngineSubscription(id) {
            sendStompFrameAsIs(StompFrame.Unsubscribe(StompUnsubscribeHeaders(id = id)))
            subscriptionsById.remove(id)
        }
    }

    override suspend fun disconnect() {
        if (config.gracefulDisconnect) {
            val frame = StompFrame.Disconnect(StompDisconnectHeaders(nextReceiptId.getStringAndInc()))
            sendStompFrame(frame)
        }
        webSocketSession.close()
    }
}

private class CallbacksAdapter<T : Any>(
    private val messageConverter: MessageConverter,
    private val clazz: KClass<T>,
    private val channel: SendChannel<KrossbowMessage<T>>
) : SubscriptionCallbacks<ByteArray> {

    override suspend fun onReceive(message: KrossbowMessage<ByteArray>) {
        channel.send(messageConverter.convertFromBytes(message, clazz))
    }

    override fun onError(throwable: Throwable) {
        channel.close(throwable)
    }
}
