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
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.frame.StompDecoder
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.frame.encodeToBytes
import org.hildan.krossbow.stomp.frame.encodeToText
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.stomp.headers.StompDisconnectHeaders
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.stomp.headers.StompUnsubscribeHeaders
import org.hildan.krossbow.utils.SuspendingAtomicInt
import org.hildan.krossbow.utils.getStringAndInc
import org.hildan.krossbow.websocket.WebSocketListener
import org.hildan.krossbow.websocket.WebSocketSession

@OptIn(ExperimentalCoroutinesApi::class) // for broadcast channel
internal class InternalStompSession(
    private val config: StompConfig,
    private val webSocketSession: WebSocketSession
) : StompSession, WebSocketListener {

    private val nextSubscriptionId = SuspendingAtomicInt(0)

    private val nextReceiptId = SuspendingAtomicInt(0)

    private val subscriptionsById: MutableMap<String, Subscription<*>> = mutableMapOf()

    private val nonMsgFrames = BroadcastChannel<StompFrame>(Channel.BUFFERED)

    init {
        webSocketSession.listener = this
    }

    // FIXME never fail in listener methods, but dispatch the decoding errors to waiting channels
    override suspend fun onTextMessage(text: String) = onFrameReceived(StompDecoder.decode(text))

    // FIXME never fail in listener methods, but dispatch the decoding errors to waiting channels
    override suspend fun onBinaryMessage(bytes: ByteArray) = onFrameReceived(StompDecoder.decode(bytes))

    override suspend fun onError(error: Throwable) {
        // TODO allow user to set a global onError listener?
        // FIXME never fail in listener methods, but dispatch the error to waiting channels
        throw IllegalStateException("Error at WebSocket level: ${error.message}", error)
    }

    override suspend fun onClose(code: Int, reason: String?) {
        // TODO allow user to set a global onClose listener?
        // FIXME dispatch the close event to waiting channels
        println("Underlying WebSocket connection closed")
    }

    private suspend fun onFrameReceived(frame: StompFrame) {
        when (frame) {
            is StompFrame.Message -> onMessageFrameReceived(frame)
            is StompFrame.Error -> {
                nonMsgFrames.send(frame)
                onErrorFrameReceived(frame)
            }
            else -> nonMsgFrames.send(frame)
        }
    }

    private suspend fun onMessageFrameReceived(frame: StompFrame.Message) {
        val subId = frame.headers.subscription
        val sub = subscriptionsById[subId] ?: error("no active subscription with ID $subId")
        sub.onMessage(frame)
    }

    private fun onErrorFrameReceived(frame: StompFrame.Error) {
        // error on all subscriptions
        subscriptionsById.values.forEach { it.onError(frame) }
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
        return prepareAndSendFrame(StompFrame.Send(headers, body))
    }

    private suspend fun prepareAndSendFrame(frame: StompFrame): StompReceipt? {
        val receiptId = getReceiptAndMaybeSetAuto(frame)
        if (receiptId == null) {
            sendStompFrameAsIs(frame)
            return null
        }
        sendAndWaitForReceipt(receiptId, frame)
        return StompReceipt(receiptId)
    }

    private suspend fun getReceiptAndMaybeSetAuto(frame: StompFrame): String? {
        if (config.autoReceipt && frame.headers.receipt == null) {
            frame.headers.receipt = nextReceiptId.getStringAndInc()
        }
        return frame.headers.receipt
    }

    private suspend fun sendStompFrameAsIs(frame: StompFrame) {
        if (frame.body is FrameBody.Binary) {
            webSocketSession.sendBinary(frame.encodeToBytes())
        } else {
            // frames without body are also sent as text because the headers are always textual
            // Also, some sockJS implementations don't support binary frames
            webSocketSession.sendText(frame.encodeToText())
        }
    }

    private suspend fun sendAndWaitForReceipt(receiptId: String, frame: StompFrame) {
        coroutineScope {
            val receiptFrame = async { waitForReceipt(receiptId) }
            sendStompFrameAsIs(frame)
            try {
                withTimeout(frame.receiptTimeout) { receiptFrame.await() }
            } catch (e: TimeoutCancellationException) {
                throw LostReceiptException(receiptId, frame.receiptTimeout, frame)
            }
        }
    }

    private suspend fun waitForReceipt(receiptId: String): StompFrame.Receipt =
            waitForTypedFrame { it.headers.receiptId == receiptId }

    private val StompFrame.receiptTimeout: Long
        get() = if (command == StompCommand.DISCONNECT) {
            config.disconnectTimeoutMillis
        } else {
            config.receiptTimeoutMillis
        }

    override suspend fun <T> subscribe(
        destination: String,
        receiptId: String?,
        convertMessage: (StompFrame.Message) -> T
    ): StompSubscription<T> {
        val id = nextSubscriptionId.getAndIncrement().toString()
        val sub = Subscription(id, convertMessage, this)
        subscriptionsById[id] = sub
        val headers = StompSubscribeHeaders(destination = destination, id = id).apply { receipt = receiptId }
        val subscribeFrame = StompFrame.Subscribe(headers)
        prepareAndSendFrame(subscribeFrame)
        return sub
    }

    internal suspend fun unsubscribe(subscriptionId: String) {
        sendStompFrameAsIs(StompFrame.Unsubscribe(StompUnsubscribeHeaders(id = subscriptionId)))
        subscriptionsById.remove(subscriptionId)
    }

    override suspend fun disconnect() {
        if (config.gracefulDisconnect) {
            sendDisconnectFrameAndWaitForReceipt()
        }
        nonMsgFrames.cancel()
        webSocketSession.close()
    }

    private suspend fun sendDisconnectFrameAndWaitForReceipt() {
        try {
            val receiptId = nextReceiptId.getStringAndInc()
            val disconnectFrame = StompFrame.Disconnect(StompDisconnectHeaders(receiptId))
            sendAndWaitForReceipt(receiptId, disconnectFrame)
        } catch (e: LostReceiptException) {
            // Sometimes the server closes the connection too quickly to send a RECEIPT, which is not really an error
            // http://stomp.github.io/stomp-specification-1.2.html#Connection_Lingering
        }
    }
}

private class Subscription<out T>(
    override val id: String,
    private val convertMessage: (StompFrame.Message) -> T,
    private val internalSession: InternalStompSession
) : StompSubscription<T> {

    private val internalMsgChannel: Channel<T> = Channel()

    override val messages: ReceiveChannel<T> get() = internalMsgChannel

    suspend fun onMessage(message: StompFrame.Message) {
        try {
            internalMsgChannel.send(convertMessage(message))
        } catch (e: Exception) {
            internalMsgChannel.close(MessageConversionException(e))
        }
    }

    fun onError(error: StompFrame.Error) {
        internalMsgChannel.close(StompErrorFrameReceived(error))
    }

    override suspend fun unsubscribe() {
        internalSession.unsubscribe(id)
        internalMsgChannel.close()
    }
}

class MessageConversionException(cause: Throwable) : Exception(cause.message, cause)
