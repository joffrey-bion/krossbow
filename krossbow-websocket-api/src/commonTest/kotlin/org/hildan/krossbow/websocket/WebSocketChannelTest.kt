package org.hildan.krossbow.websocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.hildan.krossbow.websocket.test.runSuspendingTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WebSocketChannelTest {

    @Test
    fun onTextMessage_triggersTextFrame() = runSuspendingTest {
        val adapter = WebSocketListenerChannelAdapter()

        launch { adapter.onTextMessage("test") }
        val frame = adapter.incomingFrames.receive()
        assertEquals(WebSocketFrame.Text("test"), frame)
    }

    @Test
    fun onBinaryMessage_triggersBinaryFrame() = runSuspendingTest {
        val adapter = WebSocketListenerChannelAdapter()

        launch { adapter.onBinaryMessage(ByteArray(2) { 42 }) }
        val frame = adapter.incomingFrames.receive()
        assertTrue(frame is WebSocketFrame.Binary)
        assertEquals(List<Byte>(2) { 42 }, frame.bytes.toList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onClose_triggersCloseFrameAndClosesTheChannel() = runSuspendingTest {
        val adapter = WebSocketListenerChannelAdapter()

        launch { adapter.onClose(1024, "REASON") }
        val frame = adapter.incomingFrames.receive()
        assertEquals(WebSocketFrame.Close(1024, "REASON"), frame)

        assertTrue(adapter.incomingFrames.isClosedForReceive)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onErrorText_propagatesExceptionToChannel() = runSuspendingTest {
        val adapter = WebSocketListenerChannelAdapter()

        launch { adapter.onError("some error") }

        val ex = assertFailsWith(WebSocketException::class) { adapter.incomingFrames.receive() }
        assertEquals("some error", ex.message)
        assertTrue(adapter.incomingFrames.isClosedForReceive)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun onErrorThrowable_propagatesExceptionToChannel() = runSuspendingTest {
        val adapter = WebSocketListenerChannelAdapter()

        launch { adapter.onError(RuntimeException("some error")) }

        val ex = assertFailsWith(WebSocketException::class) { adapter.incomingFrames.receive() }
        assertEquals("some error", ex.message)
        // for some reason, cause is nested twice on JVM (but not on JS targets)
        assertEquals(RuntimeException::class, (ex.cause?.cause ?: ex.cause)!!::class)
        assertTrue(adapter.incomingFrames.isClosedForReceive)
    }
}
