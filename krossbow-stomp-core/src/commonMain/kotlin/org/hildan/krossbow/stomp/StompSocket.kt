package org.hildan.krossbow.stomp

import kotlinx.coroutines.flow.*
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.*
import org.hildan.krossbow.stomp.frame.StompDecoder
import org.hildan.krossbow.stomp.heartbeats.*
import org.hildan.krossbow.websocket.*

/**
 * A [WebSocketConnection] wrapper that enables sending and receiving STOMP frames through the socket.
 * It handles the frame conversions between web sockets and STOMP.
 */
internal class StompSocket(
    private val webSocketConnection: WebSocketConnection,
    private val config: StompConfig,
) {
    /**
     * The [Flow] of decoded STOMP events. Only a single collector is expected.
     */
    val incomingEvents: Flow<StompEvent> = webSocketConnection.incomingFrames
            .catch { registerWsExceptionAndRethrow(it) }
            .map { decodeStomp(it) }
            .catch {
                close(cause = it)
                // propagate the exception to the consumers of the STOMP events
                throw it
            }

    private suspend fun registerWsExceptionAndRethrow(ex: Throwable) {
        // Upstream (websocket) errors should just propagate through the STOMP frames channel.
        // We shouldn't call StompSocket.close() because the socket is most likely already closed/failed.
        config.instrumentation?.onWebSocketClientError(ex)
        throw ex
    }

    private suspend fun decodeStomp(wsFrame: WebSocketFrame): StompEvent {
        config.instrumentation?.onWebSocketFrameReceived(wsFrame)
        val event = wsFrame.decodeToStompEvent()
        val frame = event as? StompFrame ?: return event
        config.instrumentation?.onFrameDecoded(wsFrame, frame)
        if (frame is StompFrame.Error) {
            // we throw an exception here (not a materialized error yet) because
            // we want to catch it in the main flow in order to close the websocket
            throw StompErrorFrameReceived(frame)
        }
        return frame
    }

    suspend fun sendStompFrame(frame: StompFrame) {
        if (frame.body is FrameBody.Binary) {
            webSocketConnection.sendBinary(frame.encodeToBytes())
        } else {
            // Frames without body are also sent as text because the headers are always textual.
            // Also, some sockJS implementations don't support binary frames.
            webSocketConnection.sendText(frame.encodeToText())
        }
        config.instrumentation?.onStompFrameSent(frame)
    }

    suspend fun sendHeartBeat() {
        webSocketConnection.sendHeartBeat()
    }

    suspend fun close(cause: Throwable? = null) {
        // If we are shutting down because of WebSocketClosedUnexpectedly, then we shouldn't try to close the web
        // socket again (it's literally already closed, as the exception means).
        // In case of web socket exception, there is no need to close at all because the web socket is just failed.
        // In case of STOMP ERROR frame, the server must close the connection.
        // However, the web socket did not error and we may need to close the output, so we don't discriminate
        // against StompErrorFrameReceived, and close anyway even in this case.
        if (cause !is WebSocketClosedUnexpectedly && cause !is WebSocketException) {
            webSocketConnection.close(cause)
        }
        // this is reported even if the websocket was closed unexpectedly (it doesn't have to be us closing it)
        config.instrumentation?.onWebSocketClosed(cause)
    }
}

private fun WebSocketFrame.decodeToStompEvent(): StompEvent {
    if (isHeartBeat()) {
        return StompEvent.HeartBeat
    }
    return when (this) {
        is WebSocketFrame.Text -> StompDecoder.decode(text)
        is WebSocketFrame.Binary -> StompDecoder.decode(bytes)
        is WebSocketFrame.Ping,
        is WebSocketFrame.Pong -> StompEvent.HeartBeat // we need to count this traffic
        is WebSocketFrame.Close -> throw WebSocketClosedUnexpectedly(code, reason)
    }
}

private suspend fun WebSocketConnection.close(cause: Throwable?) {
    close(
        code = closeCodeFor(cause),
        reason = cause?.message?.truncateToCloseFrameReasonLength(),
    )
}

private fun closeCodeFor(cause: Throwable?): Int = when (cause) {
    null -> WebSocketCloseCodes.NORMAL_CLOSURE
    is MissingHeartBeatException -> 3002 // 1002 would be PROTOCOL_ERROR, but browsers reserve it
    else -> 3001 // 1001 would be GOING_AWAY, but browsers reserve this code for actual page leave
}
