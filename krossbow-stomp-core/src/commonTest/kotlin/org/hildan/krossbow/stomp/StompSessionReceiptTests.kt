package org.hildan.krossbow.stomp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompReceiptHeaders
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.test.*
import org.hildan.krossbow.websocket.WebSocketCloseCodes
import org.hildan.krossbow.websocket.WebSocketException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private val TEST_RECEIPT_TIMEOUT: Duration = 500.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class StompSessionReceiptTests {

    @Test
    fun send_doesntWaitIfNoReceipt() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = false
        }
        val deferredSend = async { stompSession.sendEmptyMsg("/destination") }
        assertFalse(deferredSend.isCompleted, "send() should wait for the websocket to actually send the frame")
        wsSession.awaitSendFrameAndSimulateCompletion()
        runCurrent()
        assertTrue(deferredSend.isCompleted, "send() should resume immediately after the SEND frame is sent")
        assertNull(deferredSend.await(), "send() should return null because receipt is not used here")
    }

    @Test
    fun send_autoReceipt_waitsUntilReceipt() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
        }
        val deferredSend = async { stompSession.sendEmptyMsg("/destination") }
        val sendFrame = wsSession.awaitSendFrameAndSimulateCompletion()
        val receiptId = sendFrame.headers.receipt
        assertNotNull(receiptId, "receipt header should be auto-populated")
        assertFalse(deferredSend.isCompleted, "send() should wait until receipt is received")

        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders(receiptId)))
        runCurrent()
        assertTrue(deferredSend.isCompleted, "send() should resume when correct RECEIPT frame is received")
        val actualReceipt = deferredSend.await()
        assertEquals(StompReceipt(receiptId), actualReceipt, "send() should resume after correct receipt")
    }

    @Test
    fun send_autoReceipt_timesOutIfLostReceipt() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
            receiptTimeout = TEST_RECEIPT_TIMEOUT
        }
        val deferredSend = async {
            wsSession.awaitSendFrameAndSimulateCompletion()
        }
        val exception = assertTimesOutWith(LostReceiptException::class, TEST_RECEIPT_TIMEOUT) {
            stompSession.sendEmptyMsg("/destination")
        }
        val sendFrame = deferredSend.await()
        assertEquals(sendFrame.headers.receipt, exception.receiptId)
        assertEquals(TEST_RECEIPT_TIMEOUT, exception.configuredTimeout)
        assertEquals(sendFrame, exception.frame)
    }

    @Test
    fun send_autoReceipt_failsOnStompErrorFrame() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
        }
        launch {
            wsSession.awaitSendFrameAndSimulateCompletion()
            wsSession.simulateErrorFrameReceived("some error")
            wsSession.expectClose()
        }
        val exception = assertFailsWith(StompErrorFrameReceived::class) {
            stompSession.sendEmptyMsg("/destination")
        }
        assertEquals("some error", exception.frame.message)
    }

    @Test
    fun send_autoReceipt_failsOnWebSocketError() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
        }
        launch {
            wsSession.awaitSendFrameAndSimulateCompletion()
            wsSession.simulateError("some error")
            wsSession.expectNoClose()
        }
        val exception = assertFailsWith(WebSocketException::class) {
            stompSession.sendEmptyMsg("/destination")
        }
        assertEquals("some error", exception.message)
    }

    @Test
    fun send_autoReceipt_failsOnWebSocketClosed() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
        }
        launch {
            wsSession.awaitSendFrameAndSimulateCompletion()
            wsSession.simulateClose(WebSocketCloseCodes.NORMAL_CLOSURE, "because why not")
            wsSession.expectNoClose()
        }
        val exception = assertFailsWith(WebSocketClosedUnexpectedly::class) {
            stompSession.sendEmptyMsg("/destination")
        }
        assertEquals(WebSocketCloseCodes.NORMAL_CLOSURE, exception.code)
        assertEquals("because why not", exception.reason)
    }

    @Test
    fun send_autoReceipt_failsOnSessionDisconnected() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
            receiptTimeout = 5.minutes
        }
        launch {
            wsSession.awaitSendFrameAndSimulateCompletion()
            // we simulate a disconnection between the SEND and the RECEIPT
            launch {
                wsSession.awaitDisconnectFrameAndSimulateCompletion()
                wsSession.expectClose()
            }
            stompSession.disconnect()
        }
        assertFailsWith(SessionDisconnectedException::class) {
            stompSession.sendEmptyMsg("/destination")
        }
    }

    @Test
    fun send_manualReceipt_waitsForCorrectReceipt() = runTest {
        val (wsSession, stompSession) = connectWithMocks()
        val manualReceiptId = "my-receipt"
        val headers = StompSendHeaders(destination = "/destination") {
            receipt = manualReceiptId
        }
        val deferredSend = async { stompSession.send(headers, null) }
        assertFalse(deferredSend.isCompleted, "send() should wait until ws send finishes")
        wsSession.awaitSendFrameAndSimulateCompletion()
        assertFalse(deferredSend.isCompleted, "send() should wait until receipt is received")
        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders("other-receipt")))
        assertFalse(deferredSend.isCompleted, "send() should not resume on other receipts")
        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders(manualReceiptId)))
        runCurrent()
        assertTrue(deferredSend.isCompleted, "send() should resume when correct RECEIPT frame is received")
        val actualReceipt = deferredSend.await()
        assertEquals(StompReceipt(manualReceiptId), actualReceipt, "send() should resume with correct receipt")
    }

    @Test
    fun send_manualReceipt_timesOutIfLostReceipt() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = false
            receiptTimeout = TEST_RECEIPT_TIMEOUT
        }
        val deferredSend = async {
            wsSession.awaitSendFrameAndSimulateCompletion()
        }
        val headers = StompSendHeaders(destination = "/destination") {
            receipt = "my-receipt"
        }

        val exception = assertTimesOutWith(LostReceiptException::class, TEST_RECEIPT_TIMEOUT) {
            stompSession.send(headers, null)
        }
        val sendFrame = deferredSend.await()
        assertEquals(sendFrame.headers.receipt, exception.receiptId)
        assertEquals(TEST_RECEIPT_TIMEOUT, exception.configuredTimeout)
        assertEquals(sendFrame, exception.frame)
    }

    @Test
    fun subscribe_doesntWaitIfNoReceipt() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = false
        }
        val deferredSub = async { stompSession.subscribe("/destination") }
        assertFalse(deferredSub.isCompleted, "subscribe() should wait for the websocket to actually send the frame")
        wsSession.awaitSubscribeFrameAndSimulateCompletion()
        runCurrent()
        assertTrue(deferredSub.isCompleted, "subscribe() should resume immediately after the SUBSCRIBE frame is sent")
        deferredSub.await()
    }

    @Test
    fun subscribe_autoReceipt_waitsUntilReceipt() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
        }
        val deferredSub = async { stompSession.subscribe("/destination") }
        val sendFrame = wsSession.awaitSubscribeFrameAndSimulateCompletion()
        val receiptId = sendFrame.headers.receipt
        assertNotNull(receiptId, "receipt header should be auto-populated")
        assertFalse(deferredSub.isCompleted, "subscribe() should wait until receipt is received")
        wsSession.simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders(receiptId)))
        runCurrent()
        assertTrue(deferredSub.isCompleted, "subscribe() should resume when correct RECEIPT frame is received")
        deferredSub.await()
    }

    @Test
    fun subscribe_autoReceipt_timesOutIfLostReceipt() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = true
            receiptTimeout = TEST_RECEIPT_TIMEOUT
        }
        val deferredSub = async {
            wsSession.awaitSubscribeFrameAndSimulateCompletion()
        }
        val exception = assertTimesOutWith(LostReceiptException::class, TEST_RECEIPT_TIMEOUT) {
            stompSession.subscribe("/destination")
        }
        val subscribeFrame = deferredSub.await()
        assertEquals(subscribeFrame.headers.receipt, exception.receiptId)
        assertEquals(TEST_RECEIPT_TIMEOUT, exception.configuredTimeout)
        assertEquals(subscribeFrame, exception.frame)
    }

    @Test
    fun subscribe_manualReceipt_timesOutIfLostReceipt() = runTest {
        val (wsSession, stompSession) = connectWithMocks {
            autoReceipt = false
            receiptTimeout = TEST_RECEIPT_TIMEOUT
        }
        val deferredSub = async {
            wsSession.awaitSubscribeFrameAndSimulateCompletion()
        }
        val exception = assertTimesOutWith(LostReceiptException::class, TEST_RECEIPT_TIMEOUT) {
            stompSession.subscribe(StompSubscribeHeaders("/destination") { receipt = "my-receipt" })
        }
        val subscribeFrame = deferredSub.await()
        assertEquals(subscribeFrame.headers.receipt, exception.receiptId)
        assertEquals(TEST_RECEIPT_TIMEOUT, exception.configuredTimeout)
        assertEquals(subscribeFrame, exception.frame)
    }
}
