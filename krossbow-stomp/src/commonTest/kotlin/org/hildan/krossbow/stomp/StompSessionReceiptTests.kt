package org.hildan.krossbow.stomp

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompReceiptHeaders
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.test.ImmediatelySucceedingWebSocketClient
import org.hildan.krossbow.test.WebSocketSessionMock
import org.hildan.krossbow.test.assertCompletesSoon
import org.hildan.krossbow.test.assertTimesOutWith
import org.hildan.krossbow.test.runAsyncTestWithTimeout
import org.hildan.krossbow.test.simulateConnectedFrameReceived
import org.hildan.krossbow.test.simulateTextStompFrameReceived
import org.hildan.krossbow.test.waitForSendAndSimulateCompletion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TEST_RECEIPT_TIMEOUT: Long = 10

class StompSessionReceiptTests {

    private suspend fun connectToMock(
        configure: StompConfig.() -> Unit = {}
    ): Pair<WebSocketSessionMock, StompSession> = coroutineScope {
        val wsSession = WebSocketSessionMock()
        val stompClient = StompClient(ImmediatelySucceedingWebSocketClient(wsSession), configure)
        val session = async { stompClient.connect("dummy URL") }
        wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
        wsSession.simulateConnectedFrameReceived()
        val stompSession = session.await()
        Pair(wsSession, stompSession)
    }

    @Test
    fun send_doesntWaitIfNoReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectToMock()
        val deferredSend = async { stompSession.send("/destination") }
        assertFalse(deferredSend.isCompleted, "send() should wait for the websocket to actually send the frame")
        wsSession.waitForSendAndSimulateCompletion(StompCommand.SEND)
        assertTrue(deferredSend.isCompleted, "send() should resume immediately after the SEND frame is sent")
        assertNull(deferredSend.await(), "send() should return null because receipt is not used here")
    }

    @Test
    fun send_autoReceipt_waitsUntilReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectToMock {
            autoReceipt = true
        }
        val deferredSend = async { stompSession.send("/destination") }
        val sendFrame = wsSession.waitForSendAndSimulateCompletion(StompCommand.SEND)
        val receiptId = sendFrame.headers.receipt
        assertNotNull(receiptId, "receipt header should be auto-populated")
        assertFalse(deferredSend.isCompleted, "send() should wait until receipt is received")
        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders(receiptId)))
        val actualReceipt =
                assertCompletesSoon(deferredSend, "send() should resume soon after correct RECEIPT frame is received")
        assertEquals(StompReceipt(receiptId), actualReceipt, "send() should resume after correct receipt")
    }

    @Test
    fun send_autoReceipt_timesOutIfLostReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectToMock {
            autoReceipt = true
            receiptTimeLimit = TEST_RECEIPT_TIMEOUT
        }
        // prevents the async send() exception from failing the test
        supervisorScope {
            val deferredSend = async { stompSession.send("/destination") }
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SEND)

            assertTimesOutWith(LostReceiptException::class, TEST_RECEIPT_TIMEOUT) { deferredSend.await() }
        }
    }

    @Test
    fun send_manualReceipt_waitsForCorrectReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectToMock()
        val manualReceiptId = "my-receipt"
        val headers = StompSendHeaders(destination = "/destination")
        headers.receipt = manualReceiptId
        val deferredSend = async { stompSession.send(headers, null) }
        assertFalse(deferredSend.isCompleted, "send() should wait until ws send finishes")
        wsSession.waitForSendAndSimulateCompletion(StompCommand.SEND)
        assertFalse(deferredSend.isCompleted, "send() should wait until receipt is received")
        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders("other-receipt")))
        assertFalse(deferredSend.isCompleted, "send() should not resume on other receipts")
        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders(manualReceiptId)))
        val actualReceipt =
                assertCompletesSoon(deferredSend, "send() should resume soon after correct RECEIPT frame is received")
        assertEquals(StompReceipt(manualReceiptId), actualReceipt, "send() should resume with correct receipt")
    }

    @Test
    fun send_manualReceipt_timesOutIfLostReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectToMock {
            receiptTimeLimit = TEST_RECEIPT_TIMEOUT
        }
        // prevents the async send() exception from failing the test
        supervisorScope {
            val headers = StompSendHeaders(destination = "/destination")
            headers.receipt = "my-receipt"
            val deferredSend = async { stompSession.send(headers, null) }

            wsSession.waitForSendAndSimulateCompletion(StompCommand.SEND)

            assertTimesOutWith(LostReceiptException::class, TEST_RECEIPT_TIMEOUT) { deferredSend.await() }
        }
    }

    @Test
    fun subscribe_doesntWaitIfNoReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectToMock()
        val deferredSub = async { stompSession.subscribe<String>("/destination") }
        assertFalse(deferredSub.isCompleted, "subscribe() should wait for the websocket to actually send the frame")
        wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
        assertTrue(deferredSub.isCompleted, "subscribe() should resume immediately after the SUBSCRIBE frame is sent")
    }

    @Test
    fun subscribe_autoReceipt_waitsUntilReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectToMock {
            autoReceipt = true
        }
        val deferredSub = async { stompSession.subscribe<String>("/destination") }
        val subscribeFrame = wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
        val receiptId = subscribeFrame.headers.receipt
        assertNotNull(receiptId, "receipt header should be auto-populated")
        assertFalse(deferredSub.isCompleted, "subscribe() should wait until receipt is received")
        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders(receiptId)))
        assertCompletesSoon(deferredSub, "send() should resume soon after correct RECEIPT frame is received")
    }

    @Test
    fun subscribe_autoReceipt_timesOutIfLostReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectToMock {
            autoReceipt = true
            receiptTimeLimit = TEST_RECEIPT_TIMEOUT
        }
        // prevents the async send() exception from failing the test
        supervisorScope {
            val deferredSend = async { stompSession.subscribe<String>("/destination") }
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)

            assertTimesOutWith(LostReceiptException::class, TEST_RECEIPT_TIMEOUT) { deferredSend.await() }
        }
    }

    @Test
    fun subscribe_manualReceipt_waitsForCorrectReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectToMock()
        val manualReceiptId = "my-receipt"
        val deferredSub = async { stompSession.subscribe<String>("/destination", manualReceiptId) }
        assertFalse(deferredSub.isCompleted, "subscribe() should wait until ws send finishes")
        wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
        assertFalse(deferredSub.isCompleted, "subscribe() should wait until receipt is received")
        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders("other-receipt")))
        assertFalse(deferredSub.isCompleted, "subscribe() should not resume on other receipts")
        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders(manualReceiptId)))
        assertCompletesSoon(deferredSub, "subscribe() should resume soon after correct RECEIPT frame is received")
    }

    @Test
    fun subscribe_manualReceipt_timesOutIfLostReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectToMock {
            receiptTimeLimit = TEST_RECEIPT_TIMEOUT
        }
        // prevents the async send() exception from failing the test
        supervisorScope {
            val deferredSub = async { stompSession.subscribe<String>("/destination", "my-receipt") }
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)

            assertTimesOutWith(LostReceiptException::class, TEST_RECEIPT_TIMEOUT) { deferredSub.await() }
        }
    }
}
