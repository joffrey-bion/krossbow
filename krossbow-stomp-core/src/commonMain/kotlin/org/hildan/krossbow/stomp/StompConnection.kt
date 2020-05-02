package org.hildan.krossbow.stomp

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketSession
import kotlin.coroutines.CoroutineContext

/**
 * Base class that enables sending and receiving STOMP frames through a web socket.
 * It handles the frame conversions between web sockets and STOMP.
 * It manages heart beats and is aware of heart beat frames.
 */
internal abstract class StompConnection(
    private val webSocketSession: WebSocketSession
) : CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job

    private var heartBeater: HeartBeater? = null

    init {
        launch(CoroutineName("websocket-frames-listener")) {
            try {
                for (f in webSocketSession.incomingFrames) {
                    processWebSocketFrame(f)
                }
            } catch (e: CancellationException) {
                // If this is thrown, the canceller is already taking care of cleaning up.
                // We want to avoid running the cleanup twice because there is no reason to.
                // Also, the cleanup may not be idempotent anymore some day.
            } catch (e: Exception) {
                shutdown(e)
            }
        }
    }

    private suspend fun processWebSocketFrame(f: WebSocketFrame) {
        heartBeater?.notifyMsgReceived()
        if (f.isHeartBeat()) {
            return // not an actual STOMP frame
        }
        when (f) {
            is WebSocketFrame.Text -> processStompFrame(StompDecoder.decode(f.text))
            is WebSocketFrame.Binary -> processStompFrame(StompDecoder.decode(f.bytes))
            is WebSocketFrame.Close -> shutdown(WebSocketClosedUnexpectedly(f.code, f.reason))
        }
    }

    private suspend fun processStompFrame(frame: StompFrame) {
        if (frame is StompFrame.Connected) {
            initHeartBeats(frame.headers.heartBeat)
        }
        onStompFrameReceived(frame)
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
                shutdown(MissingHeartBeatException())
            }
        )
        heartBeater?.startIn(this)
    }

    protected abstract suspend fun onStompFrameReceived(frame: StompFrame)

    protected suspend fun sendStompFrame(frame: StompFrame) {
        if (frame.body is FrameBody.Binary) {
            webSocketSession.sendBinary(frame.encodeToBytes())
        } else {
            // Frames without body are also sent as text because the headers are always textual.
            // Also, some sockJS implementations don't support binary frames.
            webSocketSession.sendText(frame.encodeToText())
        }
        heartBeater?.notifyMsgSent()
    }

    protected open suspend fun shutdown(cause: Throwable? = null) {
        webSocketSession.close()
        job.cancelAndJoin()
    }
}
