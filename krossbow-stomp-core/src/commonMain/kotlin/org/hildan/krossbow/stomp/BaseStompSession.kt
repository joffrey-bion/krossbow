package org.hildan.krossbow.stomp

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompAbortHeaders
import org.hildan.krossbow.stomp.headers.StompAckHeaders
import org.hildan.krossbow.stomp.headers.StompBeginHeaders
import org.hildan.krossbow.stomp.headers.StompCommitHeaders
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.stomp.headers.StompDisconnectHeaders
import org.hildan.krossbow.stomp.headers.StompNackHeaders
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.stomp.headers.StompUnsubscribeHeaders
import org.hildan.krossbow.utils.generateUuid

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class) // for broadcast channel
internal class BaseStompSession(
    private val config: StompConfig,
    private val stompSocket: StompSocket
) : StompSession {
    private val job = Job()
    private val subscriptionsScope: CoroutineScope = CoroutineScope(job + CoroutineName("stomp-subscriptions"))

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
        stompSocket.stompFrames.filterIsInstance<StompFrame.Connected>().firstOrNull()
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
        stompSocket.stompFrames.filterIsInstance<StompFrame.Receipt>().firstOrNull { it.headers.receiptId == receiptId }
            ?: error("Frames channel closed unexpectedly while waiting for RECEIPT frame with id='$receiptId'")

    private val StompFrame.receiptTimeout: Long
        get() = if (command == StompCommand.DISCONNECT) {
            config.disconnectTimeoutMillis
        } else {
            config.receiptTimeoutMillis
        }

    override fun subscribe(headers: StompSubscribeHeaders): Flow<StompFrame.Message> = channelFlow {
        val id = headers.id
        subscriptionsScope.launch {
            stompSocket.stompFrames
                .filterIsInstance<StompFrame.Message>()
                .filter { it.headers.subscription == id }
                .onCompletion { close(it) }
                .catch { /* avoids crashing the scope, we already transmit exceptions through close() */ }
                .collect {
                    send(it)
                }
        }
        val subscribeFrame = StompFrame.Subscribe(headers)
        prepareHeadersAndSendFrame(subscribeFrame)

        awaitClose {
            subscriptionsScope.launch { unsubscribe(id) }
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
        subscriptionsScope.cancel()
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
