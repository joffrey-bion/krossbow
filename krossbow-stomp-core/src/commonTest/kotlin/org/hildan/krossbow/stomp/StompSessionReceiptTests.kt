package org.hildan.krossbow.stomp

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompReceiptHeaders
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.test.assertCompletesSoon
import org.hildan.krossbow.test.assertTimesOutWith
import org.hildan.krossbow.test.connectWithMocks
import org.hildan.krossbow.test.runAsyncTestWithTimeout
import org.hildan.krossbow.test.simulateErrorFrameReceived
import org.hildan.krossbow.test.simulateTextStompFrameReceived
import org.hildan.krossbow.test.waitForSendAndSimulateCompletion
import org.hildan.krossbow.websocket.WebSocketCloseCodes
import org.hildan.krossbow.websocket.WebSocketException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TEST_RECEIPT_TIMEOUT: Long = 500

class StompSessionReceiptTests {

    @Test
    fun send_doesntWaitIfNoReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()
        val deferredSend = async { stompSession.sendEmptyMsg("/destination") }
        assertFalse(deferredSend.isCompleted, "send() should wait for the websocket to actually send the frame")
        wsSession.waitForSendAndSimulateCompletion(StompCommand.SEND)
        assertTrue(deferredSend.isCompleted, "send() should resume immediately after the SEND frame is sent")
        assertNull(deferredSend.await(), "send() should return null because receipt is not used here")
    }

    @Test
    fun send_autoReceipt_waitsUntilReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
        }
        val deferredSend = async { stompSession.sendEmptyMsg("/destination") }
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
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
            receiptTimeoutMillis = TEST_RECEIPT_TIMEOUT
        }
        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SEND)
        }
        assertTimesOutWith(LostReceiptException::class, TEST_RECEIPT_TIMEOUT) {
            stompSession.sendEmptyMsg("/destination")
        }
    }

    @Test
    fun send_autoReceipt_failsOnStompErrorFrame() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
        }
        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SEND)
            wsSession.simulateErrorFrameReceived("some error")
        }
        val exception = assertFailsWith(StompErrorFrameReceived::class) {
            stompSession.sendEmptyMsg("/destination")
        }
        assertEquals("some error", exception.frame.message)
    }

    @Test
    fun send_autoReceipt_failsOnWebSocketError() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
        }
        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SEND)
            wsSession.simulateError("some error")
        }
        val exception = assertFailsWith(WebSocketException::class) {
            stompSession.sendEmptyMsg("/destination")
        }
        assertEquals("some error", exception.message)
    }

    @Test
    fun send_autoReceipt_failsOnWebSocketClosed() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
        }
        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SEND)
            wsSession.simulateClose(WebSocketCloseCodes.NORMAL_CLOSURE, "because why not")
        }
        val exception = assertFailsWith(WebSocketClosedUnexpectedly::class) {
            stompSession.sendEmptyMsg("/destination")
        }
        assertEquals(WebSocketCloseCodes.NORMAL_CLOSURE, exception.code)
        assertEquals("because why not", exception.reason)
    }

    @Test
    fun send_manualReceipt_waitsForCorrectReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()
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
        val (wsSession, stompSession) = connectWithMocks {
            receiptTimeoutMillis = TEST_RECEIPT_TIMEOUT
        }
        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SEND)
        }
        val headers = StompSendHeaders(destination = "/destination")
        headers.receipt = "my-receipt"

        assertTimesOutWith(LostReceiptException::class, TEST_RECEIPT_TIMEOUT) {
            stompSession.send(headers, null)
        }
    }

    @Test
    fun subscribe_doesntWaitIfNoReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()
        val deferredSub = async { stompSession.subscribeRaw("/destination") }
        assertFalse(deferredSub.isCompleted, "subscribe() should wait for the websocket to actually send the frame")
        wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
        assertTrue(deferredSub.isCompleted, "subscribe() should resume immediately after the SUBSCRIBE frame is sent")
    }

    @Test
    fun subscribe_autoReceipt_waitsUntilReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
        }
        val deferredSub = async { stompSession.subscribeRaw("/destination") }
        val subscribeFrame = wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
        val receiptId = subscribeFrame.headers.receipt
        assertNotNull(receiptId, "receipt header should be auto-populated")
        assertFalse(deferredSub.isCompleted, "subscribe() should wait until receipt is received")
        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders(receiptId)))
        assertCompletesSoon(deferredSub, "send() should resume soon after correct RECEIPT frame is received")
    }

    @Test
    fun subscribe_autoReceipt_timesOutIfLostReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
            receiptTimeoutMillis = TEST_RECEIPT_TIMEOUT
        }
        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
        }
        assertTimesOutWith(LostReceiptException::class, TEST_RECEIPT_TIMEOUT) {
            stompSession.subscribeRaw("/destination")
        }
    }

    @Test
    fun subscribe_manualReceipt_waitsForCorrectReceipt() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()
        val manualReceiptId = "my-receipt"
        val deferredSub = async {
            stompSession.subscribe(StompSubscribeHeaders("/destination", receipt = manualReceiptId)) { Unit }
        }
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
        val (wsSession, stompSession) = connectWithMocks {
            receiptTimeoutMillis = TEST_RECEIPT_TIMEOUT
        }
        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
        }
        assertTimesOutWith(LostReceiptException::class, TEST_RECEIPT_TIMEOUT) {
            stompSession.subscribe(StompSubscribeHeaders("/destination", receipt = "my-receipt")) { Unit }
        }
    }
}
