package org.hildan.krossbow.stomp

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompReceiptHeaders
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.test.ImmediatelySucceedingWebSocketClient
import org.hildan.krossbow.test.WebSocketSessionMock
import org.hildan.krossbow.test.runAsyncTestWithTimeout
import org.hildan.krossbow.test.simulateConnectedFrameReceived
import org.hildan.krossbow.test.simulateTextStompFrameReceived
import org.hildan.krossbow.test.waitAndAssertSentFrame
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StompSessionTest {

    private lateinit var wsSession: WebSocketSessionMock
    private lateinit var stompSession: StompSession

    @BeforeTest
    fun setup() = runAsyncTestWithTimeout {
        wsSession = WebSocketSessionMock()
        val stompClient = StompClient(ImmediatelySucceedingWebSocketClient(wsSession))
        val session = async { stompClient.connect("dummy URL") }
        wsSession.waitAndAssertSentFrame(StompCommand.CONNECT)
        wsSession.simulateConnectedFrameReceived()
        stompSession = session.await()
    }

    @Test
    fun send_doesntWaitIfNoReceipt() = runAsyncTestWithTimeout {
        val receipt = async {
            stompSession.send("/destination")
        }
        assertFalse(receipt.isCompleted)
        wsSession.waitAndAssertSentFrame(StompCommand.SEND)
        assertTrue(receipt.isCompleted)
        assertNull(receipt.await())
    }

    @Test
    fun send_waitsIfReceipt() = runAsyncTestWithTimeout {
        val receipt = async {
            val headers = StompSendHeaders(destination = "/destination")
            headers.receipt = "something"
            stompSession.send(headers, null)
        }
        assertFalse(receipt.isCompleted, "send() should wait until ws send finishes")
        wsSession.waitAndAssertSentFrame(StompCommand.SEND)
        assertFalse(receipt.isCompleted, "send() should wait until receipt is received")
        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders("another one")))
        assertFalse(receipt.isCompleted, "send() should not resume on other receipts")
        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders("something")))
        delay(10)
        assertTrue(receipt.isCompleted, "send() should resume after correct receipt")
        assertEquals(StompReceipt("something"), receipt.await())
    }
}
