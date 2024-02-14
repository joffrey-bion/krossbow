package org.hildan.krossbow.websocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketListenerFlowAdapterTest {

    @Test
    fun onTextMessage_triggersTextFrame() = runTest {
        val adapter = WebSocketListenerFlowAdapter()

        launch { adapter.onTextMessage("test") }
        val frameText = adapter.receiveText()
        assertEquals("test", frameText)
    }

    @Test
    fun onTextMessage_partialMessages() = runTest {
        val adapter = WebSocketListenerFlowAdapter()
        launch {
            adapter.onTextMessage("complete", isLast = true)

            adapter.onTextMessage("begin", isLast = false)
            adapter.onTextMessage("-end", isLast = true)

            adapter.onTextMessage("complete2", isLast = true)

            adapter.onTextMessage("1", isLast = false)
            adapter.onTextMessage("2", isLast = false)
            adapter.onTextMessage("3", isLast = true)
        }
        assertEquals("complete", adapter.receiveText())
        assertEquals("begin-end", adapter.receiveText(), "the complete msg should be sent when last part is received")
        assertEquals("complete2", adapter.receiveText())
        assertEquals("123", adapter.receiveText(), "the complete msg should be sent when last part is received")
    }

    private suspend fun WebSocketListenerFlowAdapter.receiveText(): String {
        val frame = incomingFrames.first()
        assertTrue(frame is WebSocketFrame.Text)
        return frame.text
    }

    @Test
    fun onBinaryMessage_triggersBinaryFrame() = runTest {
        val adapter = WebSocketListenerFlowAdapter()

        val twoFortyTwos = ByteString(42, 42)

        launch { adapter.onBinaryMessage(twoFortyTwos) }
        val frameBytes = adapter.receiveBytes()
        assertSame(twoFortyTwos, frameBytes)
    }

    @Test
    fun onBinaryMessage_partialMessages() = runTest {
        val adapter = WebSocketListenerFlowAdapter()

        launch {
            adapter.onBinaryMessage(ByteString(0, 1, 2), isLast = true)

            adapter.onBinaryMessage(ByteString(1, 2), isLast = false)
            adapter.onBinaryMessage(ByteString(3, 4, 5), isLast = true)

            adapter.onBinaryMessage(ByteString(1), isLast = false)
            adapter.onBinaryMessage(ByteString(2), isLast = false)
            adapter.onBinaryMessage(ByteString(3), isLast = true)
        }
        assertEquals(ByteString(0, 1, 2), adapter.receiveBytes())
        assertEquals(ByteString(1, 2, 3, 4, 5), adapter.receiveBytes())
        assertEquals(ByteString(1, 2, 3), adapter.receiveBytes())
    }

    private suspend fun WebSocketListenerFlowAdapter.receiveBytes(): ByteString {
        val frame = incomingFrames.first()
        assertTrue(frame is WebSocketFrame.Binary)
        return frame.bytes
    }

    @Test
    fun onClose_triggersCloseFrameAndCompletesTheFlow() = runTest {
        val adapter = WebSocketListenerFlowAdapter()

        launch { adapter.onClose(1024, "REASON") }
        val frame = adapter.incomingFrames.first()
        assertEquals(WebSocketFrame.Close(1024, "REASON"), frame)

        assertTrue(adapter.incomingFrames.count() == 0)
    }

    @Test
    fun onErrorText_propagatesExceptionToTheFlow() = runTest {
        val adapter = WebSocketListenerFlowAdapter()

        launch { adapter.onError("some error") }

        val ex = assertFailsWith(WebSocketException::class) { adapter.incomingFrames.first() }
        assertEquals("some error", ex.message)
    }

    @Test
    fun onErrorThrowable_propagatesWrappedExceptionToFlow() = runTest {
        val adapter = WebSocketListenerFlowAdapter()

        launch { adapter.onError(RuntimeException("some error")) }

        val ex = assertFailsWith(WebSocketException::class) { adapter.incomingFrames.first() }
        assertEquals("some error", ex.message)
        // for some reason, cause is nested twice on JVM (but not on JS targets)
        assertEquals(RuntimeException::class, (ex.cause?.cause ?: ex.cause)!!::class)
    }
}
