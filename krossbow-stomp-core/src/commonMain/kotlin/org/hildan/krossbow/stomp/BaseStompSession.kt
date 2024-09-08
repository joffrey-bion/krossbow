package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.*
import org.hildan.krossbow.stomp.headers.*
import org.hildan.krossbow.stomp.heartbeats.HeartBeater
import org.hildan.krossbow.stomp.heartbeats.NO_HEART_BEATS
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

    private val scope = CoroutineScope(CoroutineName("stomp-session") + coroutineContext)

    // extra buffer required, so we can wait for the receipt frame from within the onSubscribe
    // of some other shared flow subscription (e.g. in startSubscription())
    private val sharedStompEvents = MutableSharedFlow<StompEvent>(extraBufferCapacity = 32)

    private val heartBeater = if (heartBeat != NO_HEART_BEATS) {
        HeartBeater(
            heartBeat = heartBeat,
            tolerance = config.heartBeatTolerance,
            sendHeartBeat = {
                // The web socket could have errored or be closed, and the heart beater's job not yet be cancelled.
                // In this case, we don't want the heart beater to crash
                try {
                    stompSocket.sendHeartBeat()
                } catch(e : WebSocketException) {
                    shutdown("STOMP session failed: heart beat couldn't be sent", cause = e)
                }
            },
            onMissingHeartBeat = {
                val cause = MissingHeartBeatException(heartBeat.expectedPeriod)
                sharedStompEvents.emit(StompEvent.Error(cause))
                stompSocket.close(cause)
            },
        )
    } else {
        null
    }

    private val heartBeaterJob = heartBeater?.startIn(scope)

    init {
        scope.launch {
            stompSocket.incomingEvents
                .onEach { heartBeater?.notifyMsgReceived() }
                .materializeErrorsAndCompletion()
                .collect {
                    sharedStompEvents.emit(it)
                }
        }

        scope.launch {
            sharedStompEvents.collect {
                when (it) {
                    is StompEvent.Close -> shutdown("STOMP session disconnected")
                    is StompEvent.Error -> shutdown("STOMP session cancelled due to upstream error", cause = it.cause)
                    else -> Unit
                }
            }
        }
    }

    private suspend fun shutdown(message: String, cause: Throwable? = null) {
        // cancel heartbeats immediately to limit the chances of sending a heartbeat to a closed socket
        heartBeaterJob?.cancel()
        // let other subscribers handle the error/closure before cancelling the scope
        awaitSubscriptionsCompletion()
        scope.cancel(message, cause = cause)
    }

    private suspend fun awaitSubscriptionsCompletion() {
        withTimeoutOrNull(config.subscriptionCompletionTimeout) {
            sharedStompEvents.subscriptionCount.takeWhile { it > 0 }.collect()
        }
    }

    private suspend fun startSubscription(headers: StompSubscribeHeaders): ReceiveChannel<StompFrame.Message> {
        val subscriptionStarted = CompletableDeferred<Unit>()

        val subscriptionChannel = sharedStompEvents
            .onSubscription {
                try {
                    // ensures we are already listening for frames before sending SUBSCRIBE, so we don't miss messages
                    prepareHeadersAndSendFrame(StompFrame.Subscribe(headers))
                    subscriptionStarted.complete(Unit)
                } catch (e: Exception) {
                    subscriptionStarted.completeExceptionally(e)
                }
            }
            .dematerializeErrorsAndCompletion()
            .filterIsInstance<StompFrame.Message>()
            .filter { it.headers.subscription == headers.id }
            .produceIn(scope)

        // Ensures we actually subscribe now, to conform to the semantics of subscribe().
        // produceIn() on its own cannot guarantee that the producer coroutine has started when it returns
        subscriptionStarted.await()
        return subscriptionChannel
    }

    override suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt? {
        return prepareHeadersAndSendFrame(StompFrame.Send(headers, body))
    }

    private suspend fun prepareHeadersAndSendFrame(frame: StompFrame): StompReceipt? {
        val effectiveFrame = frame.copyWithHeaders {
            maybeSetContentLength(frame.body)
            maybeSetAutoReceipt()
        }
        val receiptId = effectiveFrame.headers.receipt
        if (receiptId == null) {
            sendStompFrame(effectiveFrame)
            return null
        }
        sendAndWaitForReceipt(receiptId, effectiveFrame)
        return StompReceipt(receiptId)
    }

    private fun StompHeaders.maybeSetContentLength(frameBody: FrameBody?) {
        if (config.autoContentLength && contentLength == null) {
            contentLength = frameBody?.bytes?.size ?: 0
        }
    }

    private fun StompHeaders.maybeSetAutoReceipt() {
        if (config.autoReceipt && receipt == null) {
            receipt = generateUuid()
        }
    }

    private suspend fun sendAndWaitForReceipt(receiptId: String, frame: StompFrame) {
        withTimeoutOrNull(frame.receiptTimeout) {
            sharedStompEvents
                .onSubscription {
                    sendStompFrame(frame)
                }
                .dematerializeErrorsAndCompletion()
                .filterIsInstance<StompFrame.Receipt>()
                .firstOrNull { it.headers.receiptId == receiptId }
                ?: throw SessionDisconnectedException("The STOMP frames flow completed unexpectedly while waiting for RECEIPT frame with id='$receiptId'")
        } ?: throw LostReceiptException(receiptId, frame.receiptTimeout, frame)
    }

    private val StompFrame.receiptTimeout: Duration
        get() = if (command == StompCommand.DISCONNECT) config.disconnectTimeout else config.receiptTimeout

    override suspend fun subscribe(headers: StompSubscribeHeaders): Flow<StompFrame.Message> {
        val headersWithId = headers.withId()

        return startSubscription(headersWithId)
            .consumeAsFlow()
            .onCompletion {
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
        sharedStompEvents.emit(StompEvent.Close)
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

private fun Flow<StompEvent>.materializeErrorsAndCompletion(): Flow<StompEvent> =
    catch { emit(StompEvent.Error(cause = it)) }
        .onCompletion { if (it == null) emit(StompEvent.Close) }

private fun Flow<StompEvent>.dematerializeErrorsAndCompletion(): Flow<StompEvent> =
    takeWhile { it !is StompEvent.Close }
        .onEach { if (it is StompEvent.Error) throw it.cause }
