package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onCompletion
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.*
import org.hildan.krossbow.utils.generateUuid

internal class BaseStompSession(
    private val config: StompConfig,
    private val stompSocket: StompSocket,
) : StompSession {

    internal suspend fun connect(headers: StompConnectHeaders): StompFrame.Connected = coroutineScope {
        val futureConnectedFrame = async(start = CoroutineStart.UNDISPATCHED) {
            waitForConnectedFrame()
        }
        val connectFrame = if (config.connectWithStompCommand) {
            StompFrame.Stomp(headers)
        } else {
            StompFrame.Connect(headers)
        }
        stompSocket.sendStompFrame(connectFrame)
        futureConnectedFrame.await()
    }

    private suspend inline fun waitForConnectedFrame(): StompFrame.Connected =
        stompSocket.stompFramesFlow.filterIsInstance<StompFrame.Connected>().firstOrNull()
            ?: error("Frames channel closed unexpectedly while expecting the CONNECTED frame")

    override suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt? {
        return prepareHeadersAndSendFrame(StompFrame.Send(headers, body))
    }

    private suspend fun prepareHeadersAndSendFrame(frame: StompFrame): StompReceipt? {
        maybeSetContentLength(frame)
        maybeSetAutoReceipt(frame)
        val receiptId = frame.headers.receipt
        if (receiptId == null) {
            stompSocket.sendStompFrame(frame)
            return null
        }
        sendAndWaitForReceipt(receiptId, frame)
        return StompReceipt(receiptId)
    }

    private fun maybeSetContentLength(frame: StompFrame) {
        if (config.autoContentLength && frame.headers.contentLength == null) {
            frame.headers.contentLength = frame.body?.bytes?.size ?: 0
        }
    }

    private fun maybeSetAutoReceipt(frame: StompFrame) {
        if (config.autoReceipt && frame.headers.receipt == null) {
            frame.headers.receipt = generateUuid()
        }
    }

    private suspend fun sendAndWaitForReceipt(receiptId: String, frame: StompFrame) {
        coroutineScope {
            val deferredReceipt = async(start = CoroutineStart.UNDISPATCHED) {
                waitForReceipt(receiptId)
            }
            stompSocket.sendStompFrame(frame)
            withTimeoutOrNull(frame.receiptTimeout) { deferredReceipt.await() }
                ?: throw LostReceiptException(receiptId, frame.receiptTimeout, frame)
        }
    }

    private suspend fun waitForReceipt(receiptId: String): StompFrame.Receipt =
        stompSocket.stompFramesFlow.filterIsInstance<StompFrame.Receipt>().firstOrNull { it.headers.receiptId == receiptId }
            ?: error("Frames channel closed unexpectedly while waiting for RECEIPT frame with id='$receiptId'")

    private val StompFrame.receiptTimeout: Long
        get() = if (command == StompCommand.DISCONNECT) {
            config.disconnectTimeoutMillis
        } else {
            config.receiptTimeoutMillis
        }

    @OptIn(ExperimentalCoroutinesApi::class) // for broadcast channel
    override suspend fun subscribe(headers: StompSubscribeHeaders): Flow<StompFrame.Message> {
        // generating the ID within the flow enables multiple concurrent collectors (because different subscription IDs)
        val headersWithId = headers.withId()
        val id = headersWithId.id

        // it's necessary to open the subscription before sending SUBSCRIBE, otherwise we may miss the first messages
        val allFrames = stompSocket.stompFramesChannel.openSubscription()
        prepareHeadersAndSendFrame(StompFrame.Subscribe(headersWithId))

        return allFrames.consumeAsFlow()
            .filterIsInstance<StompFrame.Message>()
            .filter { it.headers.subscription == id }
            .onCompletion {
                when (it) {
                    // If the consumer was cancelled or an exception occurred downstream, the STOMP session keeps going
                    // so we want to unsubscribe this failed subscription.
                    // Note that calling .first() actually cancels the flow with CancellationException, so it's
                    // covered here.
                    is CancellationException -> unsubscribe(id)
                    // If the flow completes normally, it means the frames channel is closed, and so is the web socket
                    // connection. We can't send an unsubscribe frame in this case.
                    // If an exception is thrown upstream, it means there was a STOMP or web socket error and we can't
                    // unsubscribe either.
                    else -> Unit
                }
            }
    }

    private suspend fun unsubscribe(subscriptionId: String) {
        stompSocket.sendStompFrame(StompFrame.Unsubscribe(StompUnsubscribeHeaders(id = subscriptionId)))
    }

    override suspend fun ack(ackId: String, transactionId: String?) {
        stompSocket.sendStompFrame(StompFrame.Ack(StompAckHeaders(ackId, transactionId)))
    }

    override suspend fun nack(ackId: String, transactionId: String?) {
        stompSocket.sendStompFrame(StompFrame.Nack(StompNackHeaders(ackId, transactionId)))
    }

    override suspend fun begin(transactionId: String) {
        stompSocket.sendStompFrame(StompFrame.Begin(StompBeginHeaders(transactionId)))
    }

    override suspend fun commit(transactionId: String) {
        stompSocket.sendStompFrame(StompFrame.Commit(StompCommitHeaders(transactionId)))
    }

    override suspend fun abort(transactionId: String) {
        stompSocket.sendStompFrame(StompFrame.Abort(StompAbortHeaders(transactionId)))
    }

    override suspend fun disconnect() {
        if (config.gracefulDisconnect) {
            sendDisconnectFrameAndWaitForReceipt()
        }
        stompSocket.close()
    }

    private suspend fun sendDisconnectFrameAndWaitForReceipt() {
        try {
            val receiptId = generateUuid()
            val disconnectFrame = StompFrame.Disconnect(StompDisconnectHeaders(receiptId))
            sendAndWaitForReceipt(receiptId, disconnectFrame)
        } catch (e: LostReceiptException) {
            // Sometimes the server closes the connection too quickly to send a RECEIPT, which is not really an error
            // http://stomp.github.io/stomp-specification-1.2.html#Connection_Lingering
        }
    }
}

private fun StompSubscribeHeaders.withId(): StompSubscribeHeaders {
    // we can't use the delegated id property here, because it would crash if the underlying header is absent
    val existingId = get(HeaderNames.ID)
    if (existingId != null) {
        return this
    }
    val rawHeadersCopy = HashMap(this)
    rawHeadersCopy[HeaderNames.ID] = generateUuid()
    return StompSubscribeHeaders(rawHeadersCopy.asStompHeaders())
}
