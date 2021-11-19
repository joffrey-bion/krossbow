package org.hildan.krossbow.websocket

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.hildan.krossbow.websocket.test.runSuspendingTest
import kotlin.test.*

class WebSocketListenerFlowAdapterTest {

    @Test
    fun onTextMessage_triggersTextFrame() = runSuspendingTest {
        val adapter = WebSocketListenerFlowAdapter()

        launch { adapter.onTextMessage("test") }
        val frameText = adapter.receiveText()
        assertEquals("test", frameText)
    }

    @Test
    fun onTextMessage_partialMessages() = runSuspendingTest {
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
    fun onBinaryMessage_triggersBinaryFrame() = runSuspendingTest {
        val adapter = WebSocketListenerFlowAdapter()

        launch { adapter.onBinaryMessage(ByteArray(2) { 42 }) }
        val frameBytes = adapter.receiveBytes()
        assertEquals(List<Byte>(2) { 42 }, frameBytes.toList())
    }

    @Test
    fun onBinaryMessage_partialMessages() = runSuspendingTest {
        val adapter = WebSocketListenerFlowAdapter()

        val zeroOneTwo = listOf<Byte>(0, 1, 2)
        val oneTwo = listOf<Byte>(1, 2)
        val threeFourFive = listOf<Byte>(3, 4, 5)
        val one = listOf<Byte>(1)
        val two = listOf<Byte>(2)
        val three = listOf<Byte>(3)

        launch {
            adapter.onBinaryMessage(zeroOneTwo.toByteArray(), isLast = true)

            adapter.onBinaryMessage(oneTwo.toByteArray(), isLast = false)
            adapter.onBinaryMessage(threeFourFive.toByteArray(), isLast = true)

            adapter.onBinaryMessage(one.toByteArray(), isLast = false)
            adapter.onBinaryMessage(two.toByteArray(), isLast = false)
            adapter.onBinaryMessage(three.toByteArray(), isLast = true)
        }
        assertEquals(zeroOneTwo, adapter.receiveBytes().toList())
        assertEquals(oneTwo + threeFourFive, adapter.receiveBytes().toList())
        assertEquals(one + two + three, adapter.receiveBytes().toList())
    }

    private suspend fun WebSocketListenerFlowAdapter.receiveBytes(): ByteArray {
        val frame = incomingFrames.first()
        assertTrue(frame is WebSocketFrame.Binary)
        return frame.bytes
    }

    @Test
    fun onClose_triggersCloseFrameAndCompletesTheFlow() = runSuspendingTest {
        val adapter = WebSocketListenerFlowAdapter()

        launch { adapter.onClose(1024, "REASON") }
        val frame = adapter.incomingFrames.first()
        assertEquals(WebSocketFrame.Close(1024, "REASON"), frame)

        assertTrue(adapter.incomingFrames.count() == 0)
    }

    @Test
    fun onErrorText_propagatesExceptionToTheFlow() = runSuspendingTest {
        val adapter = WebSocketListenerFlowAdapter()

        launch { adapter.onError("some error") }

        val ex = assertFailsWith(WebSocketException::class) { adapter.incomingFrames.first() }
        assertEquals("some error", ex.message)
    }

    @Test
    fun onErrorThrowable_propagatesWrappedExceptionToFlow() = runSuspendingTest {
        val adapter = WebSocketListenerFlowAdapter()

        launch { adapter.onError(RuntimeException("some error")) }

        val ex = assertFailsWith(WebSocketException::class) { adapter.incomingFrames.first() }
        assertEquals("some error", ex.message)
        // for some reason, cause is nested twice on JVM (but not on JS targets)
        assertEquals(RuntimeException::class, (ex.cause?.cause ?: ex.cause)!!::class)
    }
}
