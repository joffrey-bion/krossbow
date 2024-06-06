package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.hildan.krossbow.stomp.frame.*
import org.hildan.krossbow.stomp.headers.*
import org.hildan.krossbow.test.*
import kotlin.test.*

class StompSessionContentLengthTests {

    @Test
    fun send_noContentLength() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoContentLength = false
        }
        launch { stompSession.sendText("/destination", "something") }
        val frame = wsSession.awaitSendFrameAndSimulateCompletion()
        assertNull(frame.headers.contentLength)

        launch { stompSession.sendEmptyMsg("/destination") }
        val emptyFrame = wsSession.awaitSendFrameAndSimulateCompletion()
        assertNull(emptyFrame.headers.contentLength)
    }

    @Test
    fun send_manualContentLength_correct() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoContentLength = false
        }
        val headers = StompSendHeaders("/destination").apply {
            contentLength = 9
        }
        launch { stompSession.send(headers, FrameBody.Text("something")) }
        val frame1 = wsSession.awaitSendFrameAndSimulateCompletion()
        assertEquals(9, frame1.headers.contentLength)
    }

    @Test
    fun send_manualContentLength_tooShort() = runTest {
        val (_, stompSession) = connectWithMocks {
            autoContentLength = false
        }
        val headers = StompSendHeaders("/destination").apply {
            contentLength = 5
        }
        assertFailsWith<InvalidContentLengthException> {
            stompSession.send(headers, FrameBody.Text("something"))
        }
    }

    @Test
    fun send_manualContentLength_tooLong() = runTest {
        val (_, stompSession) = connectWithMocks {
            autoContentLength = false
        }
        val headers = StompSendHeaders("/destination").apply {
            contentLength = 100
        }
        assertFailsWith<InvalidContentLengthException> {
            stompSession.send(headers, FrameBody.Text("something"))
        }
    }

    @Test
    fun send_autoContentLength() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoContentLength = true
        }
        launch { stompSession.sendText("/destination", "something") }
        val frame = wsSession.awaitSendFrameAndSimulateCompletion()
        assertEquals(9, frame.headers.contentLength)

        launch { stompSession.sendEmptyMsg("/destination") }
        val emptyFrame = wsSession.awaitSendFrameAndSimulateCompletion()
        assertEquals(0, emptyFrame.headers.contentLength)
    }

    @Test
    fun send_autoContentLength_canReuseHeaders() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoContentLength = true
        }
        val headers = StompSendHeaders("/destination")
        launch { stompSession.send(headers, FrameBody.Text("something")) }
        val frame1 = wsSession.awaitSendFrameAndSimulateCompletion()
        assertEquals(9, frame1.headers.contentLength)

        launch { stompSession.send(headers, FrameBody.Text("something bigger")) }
        val frame2 = wsSession.awaitSendFrameAndSimulateCompletion()
        assertEquals(16, frame2.headers.contentLength)
    }
}