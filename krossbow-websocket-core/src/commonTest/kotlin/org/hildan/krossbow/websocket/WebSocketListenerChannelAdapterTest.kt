package org.hildan.krossbow.websocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.hildan.krossbow.websocket.test.runSuspendingTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WebSocketListenerChannelAdapterTest {

    @Test
    fun onTextMessage_triggersTextFrame() = runSuspendingTest {
        val adapter = WebSocketListenerChannelAdapter()

        launch { adapter.onTextMessage("test") }
        val frame = adapter.incomingFrames.receive()
        assertEquals(WebSocketFrame.Text("test"), frame)
    }

    @Test
    fun onTextMessage_partialMessages() = runSuspendingTest {
        val adapter = WebSocketListenerChannelAdapter()
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

    private suspend fun WebSocketListenerChannelAdapter.receiveText(): String {
        val frame = incomingFrames.receive()
        assertTrue(frame is WebSocketFrame.Text)
        return frame.text
    }

    @Test
    fun onBinaryMessage_triggersBinaryFrame() = runSuspendingTest {
        val adapter = WebSocketListenerChannelAdapter()

        launch { adapter.onBinaryMessage(ByteArray(2) { 42 }) }
        val frame = adapter.incomingFrames.receive()
        assertTrue(frame is WebSocketFrame.Binary)
        assertEquals(List<Byte>(2) { 42 }, frame.bytes.toList())
    }

    @Test
    fun onBinaryMessage_partialMessages() = runSuspendingTest {
        val adapter = WebSocketListenerChannelAdapter()

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

    private suspend fun WebSocketListenerChannelAdapter.receiveBytes(): ByteArray {
        val frame = incomingFrames.receive()
        assertTrue(frame is WebSocketFrame.Binary)
        return frame.bytes
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
