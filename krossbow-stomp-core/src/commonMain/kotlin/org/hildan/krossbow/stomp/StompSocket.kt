package org.hildan.krossbow.stomp

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompDecoder
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.frame.encodeToBytes
import org.hildan.krossbow.stomp.frame.encodeToText
import org.hildan.krossbow.stomp.heartbeats.*
import org.hildan.krossbow.stomp.heartbeats.HeartBeater
import org.hildan.krossbow.stomp.heartbeats.isHeartBeat
import org.hildan.krossbow.stomp.heartbeats.sendHeartBeat
import org.hildan.krossbow.websocket.WebSocketCloseCodes
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketSession
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A web socket wrapper that enables sending and receiving STOMP frames through the socket.
 * It handles the frame conversions between web sockets and STOMP.
 * It manages heart beats and is aware of heart beat frames.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class StompSocket(
    private val webSocketSession: WebSocketSession,
    private val config: StompConfig,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) {
    private val scope = CoroutineScope(coroutineContext + Job() + CoroutineName("stomp-socket"))

    private var heartBeater: HeartBeater? = null

    /**
     * The [BroadcastChannel] of decoded STOMP frames.
     */
    val stompFramesChannel = BroadcastChannel<StompFrame>(Channel.BUFFERED)

    /**
     * The [Flow] of decoded STOMP frames.
     *
     * Multiple concurrent collectors are allowed and correspond to independent subscriptions.
     * All frames are sent to all subscribers similarly to a [BroadcastChannel].
     * Cancellation of one collector results in the cancellation of the corresponding subscription but doesn't fail
     * the others.
     */
    val stompFramesFlow: Flow<StompFrame> = stompFramesChannel.asFlow()

    init {
        scope.launch(CoroutineName("stomp-frame-decoder")) {
            webSocketSession.incomingFrames.consumeAsFlow()
                .catch { processUpstreamWebsocketException(it) }
                .onEach { processWebSocketFrame(it) }
                .catch { close(it) }
                .collect()
        }
    }

    private suspend fun processUpstreamWebsocketException(ex: Throwable) {
        // Upstream (websocket) errors should just propagate through the STOMP frames channel.
        // We shouldn't call StompSocket.close() because the socket is most likely already closed/failed.
        stompFramesChannel.close(ex)
        config.instrumentation?.onWebSocketClientError(ex)
    }

    private suspend fun processWebSocketFrame(wsFrame: WebSocketFrame) {
        heartBeater?.notifyMsgReceived()
        config.instrumentation?.onWebSocketFrameReceived(wsFrame)
        if (wsFrame.isHeartBeat()) {
            return // not an actual STOMP frame
        }
        val f = decodeFrame(wsFrame) ?: return // ping/pong frame
        config.instrumentation?.onFrameDecoded(wsFrame, f)
        if (f is StompFrame.Error) {
            throw StompErrorFrameReceived(f)
        }
        if (f is StompFrame.Connected) {
            initHeartBeats(f.headers.heartBeat)
        }
        stompFramesChannel.send(f)
    }

    private fun decodeFrame(f: WebSocketFrame): StompFrame? = when (f) {
        is WebSocketFrame.Text -> StompDecoder.decode(f.text)
        is WebSocketFrame.Binary -> StompDecoder.decode(f.bytes)
        is WebSocketFrame.Ping, is WebSocketFrame.Pong -> null
        is WebSocketFrame.Close -> throw WebSocketClosedUnexpectedly(f.code, f.reason)
    }

    private fun initHeartBeats(serverHeartBeat: HeartBeat?) {
        val negotiatedHeartBeat = config.heartBeat.negotiated(serverHeartBeat)
        if (negotiatedHeartBeat == NO_HEART_BEATS) {
            return
        }
        heartBeater = HeartBeater(
            heartBeat = negotiatedHeartBeat,
            tolerance = config.heartBeatTolerance,
            sendHeartBeat = { webSocketSession.sendHeartBeat() },
            onMissingHeartBeat = { close(MissingHeartBeatException(negotiatedHeartBeat.expectedPeriodMillis)) }
        )
        heartBeater?.startIn(scope)
    }

    suspend fun sendStompFrame(frame: StompFrame) {
        if (frame.body is FrameBody.Binary) {
            webSocketSession.sendBinary(frame.encodeToBytes())
        } else {
            // Frames without body are also sent as text because the headers are always textual.
            // Also, some sockJS implementations don't support binary frames.
            webSocketSession.sendText(frame.encodeToText())
        }
        heartBeater?.notifyMsgSent()
        config.instrumentation?.onStompFrameSent(frame)
    }

    suspend fun close(cause: Throwable? = null) {
        // If we are shutting down because of WebSocketClosedUnexpectedly, then we shouldn't try to close the web
        // socket again.
        // In case of STOMP ERROR frame, the server must close the connection.
        // However, the web socket did not error and we may need to close the output, so we don't discriminate
        // against StompErrorFrameReceived, and close anyway even in this case.
        if (cause !is WebSocketClosedUnexpectedly) {
            webSocketSession.close(code = closeCodeFor(cause), reason = cause?.message)
        }
        // this is reported even if the websocket was closed unexpectedly (it doesn't have to be us closing it)
        config.instrumentation?.onWebSocketClosed(cause)

        // Required to stop the subscribers of the frames, and propagate the exception if present.
        // We close this channel after the web socket, so that consumers can see they can't send UNSUBSCRIBE.
        stompFramesChannel.close(cause)

        // Cancels the collection of web socket frames.
        // Maybe this will happen before the web socket server's close frame is received, but it's OK because we
        // closed the stompFramesChannel already.
        scope.cancel(CancellationException(cause?.message, cause))
    }

    private fun closeCodeFor(cause: Throwable?): Int = when (cause) {
        null -> WebSocketCloseCodes.NORMAL_CLOSURE
        is MissingHeartBeatException -> 3002 // 1002 would be PROTOCOL_ERROR, but browsers reserve it
        else -> 3001 // 1001 would be GOING_AWAY, but browsers reserve this code for actual page leave
    }
}
