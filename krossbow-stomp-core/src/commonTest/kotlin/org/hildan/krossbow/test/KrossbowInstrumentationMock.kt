package org.hildan.krossbow.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.instrumentation.KrossbowInstrumentation
import org.hildan.krossbow.websocket.WebSocketFrame
import kotlin.test.fail

class KrossbowInstrumentationMock : KrossbowInstrumentation {

    private val wsFramesChannel = Channel<WebSocketFrame>()
    private val decodedFramesChannel = Channel<StompFrame>()
    private val sentFramesChannel = Channel<StompFrame>()
    private val closeCausesChannel = Channel<CloseEvent>()
    private val wsErrorsChannel = Channel<Throwable>()

    override suspend fun onWebSocketFrameReceived(frame: WebSocketFrame) {
        wsFramesChannel.send(frame)
    }

    override suspend fun onFrameDecoded(originalFrame: WebSocketFrame, decodedFrame: StompFrame) {
        decodedFramesChannel.send(decodedFrame)
    }

    override suspend fun onStompFrameSent(frame: StompFrame) {
        sentFramesChannel.send(frame)
    }

    override suspend fun onWebSocketClosed(cause: Throwable?) {
        closeCausesChannel.send(CloseEvent(cause))
    }

    override suspend fun onWebSocketClientError(exception: Throwable) {
        wsErrorsChannel.send(exception)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun expectOnWebsocketFrameReceived(): WebSocketFrame {
        return wsFramesChannel.expectElement("Expected onWebSocketFrameReceived to be called")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun expectOnFrameDecoded(): StompFrame {
        return decodedFramesChannel.expectElement("Expected onFrameDecoded to be called")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun expectOnStompFrameSent(): StompFrame {
        return sentFramesChannel.expectElement("Expected onStompFrameSent to be called")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun expectOnWebSocketClosed(): Throwable? {
        val closeEvent = closeCausesChannel.expectElement("Expected onWebSocketClosed to be called")
        return closeEvent.cause
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun expectOnWebSocketClientError(): Throwable {
        return wsErrorsChannel.expectElement("Expected onWebSocketClientError to be called")
    }

    private suspend fun <T> Channel<T>.expectElement(message: String) = receiveCatching().getOrNull() ?: fail(message)
}

private data class CloseEvent(val cause: Throwable?)
