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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompDecoder
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.frame.encodeToBytes
import org.hildan.krossbow.stomp.frame.encodeToText
import org.hildan.krossbow.stomp.heartbeats.HeartBeater
import org.hildan.krossbow.stomp.heartbeats.closeForMissingHeartBeat
import org.hildan.krossbow.stomp.heartbeats.isHeartBeat
import org.hildan.krossbow.stomp.heartbeats.sendHeartBeat
import org.hildan.krossbow.websocket.WebSocketCloseCodes
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketSession

/**
 * A web socket wrapper that enables sending and receiving STOMP frames through the socket.
 * It handles the frame conversions between web sockets and STOMP.
 * It manages heart beats and is aware of heart beat frames.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class StompSocket(
    private val webSocketSession: WebSocketSession
) {
    private val scope = CoroutineScope(Job() + CoroutineName("stomp-socket"))

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

    val canSend: Boolean
        get() = webSocketSession.canSend

    init {
        scope.launch(CoroutineName("stomp-frame-decoder")) {
            webSocketSession.incomingFrames.consumeAsFlow()
                .onEach { processWebSocketFrame(it) }
                .catch { close(it) }
                .collect()
        }
    }

    private suspend fun processWebSocketFrame(wsFrame: WebSocketFrame) {
        heartBeater?.notifyMsgReceived()
        if (wsFrame.isHeartBeat()) {
            return // not an actual STOMP frame
        }
        val f = decodeFrame(wsFrame)
        if (f is StompFrame.Error) {
            throw StompErrorFrameReceived(f)
        }
        if (f is StompFrame.Connected) {
            initHeartBeats(f.headers.heartBeat)
        }
        stompFramesChannel.send(f)
    }

    private fun decodeFrame(f: WebSocketFrame): StompFrame = when (f) {
        is WebSocketFrame.Text -> StompDecoder.decode(f.text)
        is WebSocketFrame.Binary -> StompDecoder.decode(f.bytes)
        is WebSocketFrame.Close -> throw WebSocketClosedUnexpectedly(f.code, f.reason)
    }

    private fun initHeartBeats(heartBeat: HeartBeat?) {
        if (heartBeat == null || (heartBeat.expectedPeriodMillis == 0 && heartBeat.minSendPeriodMillis == 0)) {
            return // no heart beats to set
        }
        heartBeater = HeartBeater(
            heartBeat = heartBeat,
            sendHeartBeat = { webSocketSession.sendHeartBeat() },
            onMissingHeartBeat = {
                webSocketSession.closeForMissingHeartBeat()
                close(MissingHeartBeatException(heartBeat.expectedPeriodMillis))
            }
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
    }

    suspend fun close(cause: Throwable? = null) {
        // required to stop the subscribers of the frames, and propagate the exception if present
        stompFramesChannel.close(cause)
        val closeCode = when (cause) {
            is MissingHeartBeatException -> WebSocketCloseCodes.PROTOCOL_ERROR
            null -> WebSocketCloseCodes.NORMAL_CLOSURE
            else -> WebSocketCloseCodes.GOING_AWAY
        }
        webSocketSession.close(code = closeCode, reason = cause?.message)
        // this will cancel the collection of web socket frames, maybe before the web socket server's close frame is
        // received
        scope.cancel(CancellationException(cause?.message, cause))
    }
}
