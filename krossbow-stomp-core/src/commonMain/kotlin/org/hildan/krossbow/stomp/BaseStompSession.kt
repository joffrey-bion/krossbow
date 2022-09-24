@file:OptIn(InternalKrossbowApi::class)

package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.*
import org.hildan.krossbow.stomp.headers.*
import org.hildan.krossbow.stomp.heartbeats.HeartBeater
import org.hildan.krossbow.stomp.heartbeats.NO_HEART_BEATS
import org.hildan.krossbow.stomp.utils.ConcurrentMap
import org.hildan.krossbow.stomp.utils.generateUuid
import org.hildan.krossbow.websocket.WebSocketException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

internal class BaseStompSession(
    private val config: StompConfig,
    private val stompSocket: StompSocket,
    heartBeat: HeartBeat,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
) : StompSession {

    private val scope = CoroutineScope(coroutineContext + CoroutineName("stomp-session"))

    private val heartBeater = if (heartBeat != NO_HEART_BEATS) {
        HeartBeater(
            heartBeat = heartBeat,
            tolerance = config.heartBeatTolerance,
            sendHeartBeat = {
                // The web socket could have errored or be closed, and the heart beater's job not yet be cancelled.
                // In this case, we don't want the heart beater to crash
                try {
                    stompSocket.sendHeartBeat()
                } catch (e: WebSocketException) {
                    shutdown("STOMP session terminated: heart beat couldn't be sent", cause = e)
                }
            },
            onMissingHeartBeat = {
                val cause = MissingHeartBeatException(heartBeat.expectedPeriod)
                shutdown("STOMP session terminated: no heart beat received in time", cause = cause)
                stompSocket.close(cause)
            },
        )
    } else {
        null
    }

    private val subscriptionsById = ConcurrentMap<String, Channel<StompFrame.Message>>()
    private val receipts = ConcurrentMap<String, CompletableDeferred<StompFrame.Receipt>>()

    private val heartBeaterJob = heartBeater?.startIn(scope)

    init {
        scope.launch {
            stompSocket.incomingEvents
                .catch { shutdown("STOMP session terminated due to upstream error", cause = it) }
                .collect {
                    heartBeater?.notifyMsgReceived()
                    when (it) {
                        is StompEvent.HeartBeat -> Unit // ignore, already notified
                        is StompFrame -> processStompFrame(it)
                    }
                }
            shutdown("STOMP session disconnected")
        }
    }

    private suspend fun processStompFrame(frame: StompFrame) {
        when (frame) {
            is StompFrame.Message -> processSubscriptionMessage(frame)
            is StompFrame.Receipt -> confirmReceipt(frame)
            is StompFrame.Error -> shutdown("STOMP session terminated: ERROR frame received", StompErrorFrameReceived(frame))
            else -> shutdown("STOMP session terminated: unexpected ${frame.command} frame received")
        }
    }

    private suspend fun processSubscriptionMessage(frame: StompFrame.Message) {
        subscriptionsById.get(frame.headers.subscription)?.send(frame)
    }

    private suspend fun confirmReceipt(frame: StompFrame.Receipt) {
        receipts.get(frame.headers.receiptId)?.complete(frame)
            ?: error("Missing deferred receipt for receipt ID ${frame.headers.receiptId}")
    }

    private suspend fun shutdown(message: String, cause: Throwable? = null) {
        // cancel heartbeats immediately to limit the chances of sending a heartbeat to a closed socket
        heartBeaterJob?.cancel()

        subscriptionsById.values().forEach { sub -> sub.close(cause = cause) }
        receipts.values().forEach { r -> r.completeExceptionally(cause ?: CancellationException(message)) }

        scope.cancel(message, cause = cause)
    }

    override suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt? {
        return prepareHeadersAndSendFrame(StompFrame.Send(headers, body))
    }

    private suspend fun prepareHeadersAndSendFrame(frame: StompFrame): StompReceipt? {
        maybeSetContentLength(frame)
        maybeSetAutoReceipt(frame)
        val receiptId = frame.headers.receipt
        if (receiptId == null) {
            sendStompFrame(frame)
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
        val deferredReceipt = CompletableDeferred<StompFrame.Receipt>()
        receipts.put(receiptId, deferredReceipt)
        stompSocket.sendStompFrame(frame)
        withTimeoutOrNull(frame.receiptTimeout) {
            deferredReceipt.await()
            receipts.remove(receiptId)
        } ?: throw LostReceiptException(receiptId, frame.receiptTimeout, frame)
    }

    private val StompFrame.receiptTimeout: Duration
        get() = if (command == StompCommand.DISCONNECT) config.disconnectTimeout else config.receiptTimeout

    override suspend fun subscribe(headers: StompSubscribeHeaders): Flow<StompFrame.Message> {
        val headersWithId = headers.withId()

        val subscriptionChannel = Channel<StompFrame.Message>(BUFFERED)
        subscriptionsById.put(headersWithId.id, subscriptionChannel)
        prepareHeadersAndSendFrame(StompFrame.Subscribe(headersWithId))
        return subscriptionChannel.consumeAsFlow().onCompletion {
                when (it) {
                    // If the consumer was cancelled or an exception occurred downstream, the STOMP session keeps going
                    // so we want to unsubscribe this failed subscription.
                    // Note that calling .first() actually cancels the flow with CancellationException, so it's
                    // covered here.
                    is CancellationException -> {
                        if (scope.isActive) {
                            unsubscribe(headersWithId.id)
                        } else {
                            // The whole session is cancelled, the web socket must be already closed
                        }
                    }
                    // If the flow completes normally, it means the frames channel is closed, and so is the web socket
                    // connection. We can't send an unsubscribe frame in this case.
                    // If an exception is thrown upstream, it means there was a STOMP or web socket error and we can't
                    // unsubscribe either.
                    else -> Unit
                }
            }
    }

    private suspend fun unsubscribe(subscriptionId: String) {
        sendStompFrame(StompFrame.Unsubscribe(StompUnsubscribeHeaders(id = subscriptionId)))
        subscriptionsById.get(subscriptionId)?.close()
        subscriptionsById.remove(subscriptionId)
    }

    override suspend fun ack(ackId: String, transactionId: String?) {
        sendStompFrame(StompFrame.Ack(StompAckHeaders(ackId, transactionId)))
    }

    override suspend fun nack(ackId: String, transactionId: String?) {
        sendStompFrame(StompFrame.Nack(StompNackHeaders(ackId, transactionId)))
    }

    override suspend fun begin(transactionId: String) {
        sendStompFrame(StompFrame.Begin(StompBeginHeaders(transactionId)))
    }

    override suspend fun commit(transactionId: String) {
        sendStompFrame(StompFrame.Commit(StompCommitHeaders(transactionId)))
    }

    override suspend fun abort(transactionId: String) {
        sendStompFrame(StompFrame.Abort(StompAbortHeaders(transactionId)))
    }

    private suspend fun sendStompFrame(frame: StompFrame) {
        stompSocket.sendStompFrame(frame)
        heartBeater?.notifyMsgSent()
    }

    override suspend fun disconnect() {
        if (config.gracefulDisconnect) {
            sendDisconnectFrameAndWaitForReceipt()
        }
        stompSocket.close()
        shutdown("STOMP session disconnected")
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
