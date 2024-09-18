package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.hildan.krossbow.stomp.frame.InvalidStompFrameException
import org.hildan.krossbow.stomp.headers.AckMode
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.test.*
import org.hildan.krossbow.websocket.WebSocketCloseCodes
import org.hildan.krossbow.websocket.WebSocketException
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class StompSessionSubscriptionsTest {

    @Test
    fun subscribe_suspendsUntilSubscribeFrameIsSent() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        val deferredFlow = async(start = CoroutineStart.UNDISPATCHED) { stompSession.subscribeText("/dest") }
        runCurrent()
        assertFalse(deferredFlow.isCompleted, "subscribe() should not return until SUBSCRIBE frame is sent")
        wsSession.awaitSubscribeFrameAndSimulateCompletion()
        runCurrent()
        assertTrue(deferredFlow.isCompleted, "subscribe() should return when SUBSCRIBE frame is sent")

        launch {
            wsSession.awaitDisconnectFrameAndSimulateCompletion()
            wsSession.expectClose()
        }
        stompSession.disconnect()
    }

    @Test
    fun subscribeWithReceipt_suspendsUntilReceiptIsReceived() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        val receiptId = "my-receipt"
        val deferredFlow = async(start = CoroutineStart.UNDISPATCHED) {
            val headers = StompSubscribeHeaders(destination = "/dest") { receipt = receiptId }
            stompSession.subscribe(headers)
        }
        runCurrent()
        assertFalse(deferredFlow.isCompleted, "subscribe() should not return until SUBSCRIBE frame is sent")
        wsSession.awaitSubscribeFrameAndSimulateCompletion()
        runCurrent()
        assertFalse(deferredFlow.isCompleted, "subscribe() should not return until the RECEIPT is received")
        wsSession.simulateReceiptFrameReceived("not-my-receipt")
        runCurrent()
        assertFalse(deferredFlow.isCompleted, "subscribe() should not return until the correct RECEIPT is received")
        wsSession.simulateReceiptFrameReceived(receiptId)
        runCurrent()
        assertTrue(deferredFlow.isCompleted, "subscribe() call should return when SUBSCRIBE frame is sent")

        launch {
            wsSession.awaitDisconnectFrameAndSimulateCompletion()
            wsSession.expectClose()
        }
        stompSession.disconnect()
    }

    @Test
    fun subscribe_doesntLoseMessagesIfFlowIsNotCollectedImmediately() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        val subFrame = async { wsSession.awaitSubscribeFrameAndSimulateCompletion() }
        val messages = stompSession.subscribeText("/dest")

        val subId = subFrame.await().headers.id
        wsSession.simulateMessageFrameReceived(subId, "HELLO")

        launch {
            wsSession.awaitUnsubscribeFrameAndSimulateCompletion(subId)
            wsSession.awaitDisconnectFrameAndSimulateCompletion()
            wsSession.expectClose()
        }

        advanceUntilIdle() // leave some time for the frame to potentially be lost
        val message = messages.first()
        assertEquals("HELLO", message)
        stompSession.disconnect()
    }

    @Test
    fun subscribeSendCollect_shouldNotLoseMessages() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            val subFrame = wsSession.awaitSubscribeFrameAndSimulateCompletion()
            val subId = subFrame.headers.id

            // we simulate that a SEND frame triggers a MESSAGE frame on the subscription
            val sendFrame = wsSession.awaitSendFrameAndSimulateCompletion()
            wsSession.simulateMessageFrameReceived(subId, sendFrame.bodyAsText)

            wsSession.awaitUnsubscribeFrameAndSimulateCompletion(subId)
            wsSession.awaitDisconnectFrameAndSimulateCompletion()
            wsSession.expectClose()
        }
        val messages = stompSession.subscribeText("/sub")
        stompSession.sendText("/send", "HELLO")

        delay(50) // leave some time for the frame to potentially be lost
        val message = messages.first()
        assertEquals("HELLO", message)
        stompSession.disconnect()
    }

    @Test
    fun subscribe_headersAreRespected_customId() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        val customHeaderKey = "my-header"
        val headers = StompSubscribeHeaders(destination = "/dest") {
            id = "some_id"
            ack = AckMode.CLIENT
            this[customHeaderKey] = "my-header-value"
        }
        launch {
            stompSession.subscribe(headers).first()
            stompSession.disconnect()
        }
        val subFrame = wsSession.awaitSubscribeFrameAndSimulateCompletion()
        assertEquals(headers.id, subFrame.headers.id)
        assertEquals(headers.destination, subFrame.headers.destination)
        assertEquals(headers.ack, subFrame.headers.ack)
        assertEquals(headers[customHeaderKey], subFrame.headers[customHeaderKey])
        wsSession.simulateMessageFrameReceived(subFrame.headers.id, "HELLO")
        wsSession.awaitUnsubscribeFrameAndSimulateCompletion(subFrame.headers.id)
        wsSession.awaitDisconnectFrameAndSimulateCompletion()
        wsSession.expectClose()
    }

    @Test
    fun subscribe_headersAreRespected_generatedId() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        val customHeaderKey = "my-header"
        val headers = StompSubscribeHeaders(destination = "/dest") {
            ack = AckMode.CLIENT
            this[customHeaderKey] = "my-header-value"
        }

        launch {
            stompSession.subscribe(headers).first()
            stompSession.disconnect()
        }

        val subFrame = wsSession.awaitSubscribeFrameAndSimulateCompletion()
        assertEquals(headers.id, subFrame.headers.id)
        assertEquals(headers.destination, subFrame.headers.destination)
        assertEquals(headers.ack, subFrame.headers.ack)
        assertEquals(headers[customHeaderKey], subFrame.headers[customHeaderKey])

        wsSession.simulateMessageFrameReceived(subFrame.headers.id, "HELLO")
        wsSession.awaitUnsubscribeFrameAndSimulateCompletion(subFrame.headers.id)
        wsSession.awaitDisconnectFrameAndSimulateCompletion()
        wsSession.expectClose()
    }

    @Test
    fun subscription_firstOperatorUnsubscribes() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            val subFrame = wsSession.awaitSubscribeFrameAndSimulateCompletion()
            wsSession.simulateMessageFrameReceived(subFrame.headers.id, "HELLO")
            wsSession.awaitUnsubscribeFrameAndSimulateCompletion(subFrame.headers.id)
            wsSession.awaitDisconnectFrameAndSimulateCompletion()
            wsSession.expectClose()
        }
        val messages = stompSession.subscribeText("/dest")
        val message = messages.first()
        assertEquals("HELLO", message)
        assertFalse(wsSession.closed, "Unsubscribe should not close the web socket session")

        stompSession.disconnect()
        assertTrue(wsSession.closed, "disconnect() should close the web socket session")
    }

    @Test
    fun subscription_takeOperatorUnsubscribes() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            val subFrame = wsSession.awaitSubscribeFrameAndSimulateCompletion()
            repeat(3) {
                wsSession.simulateMessageFrameReceived(subFrame.headers.id, "MSG_$it")
            }
            wsSession.awaitUnsubscribeFrameAndSimulateCompletion(subFrame.headers.id)
            wsSession.awaitDisconnectFrameAndSimulateCompletion()
            wsSession.expectClose()
        }
        val messagesFlow = stompSession.subscribeText("/dest")
        val messages = messagesFlow.take(3).toList()
        assertEquals(listOf("MSG_0", "MSG_1", "MSG_2"), messages)
        assertFalse(wsSession.closed, "Unsubscribe should not close the web socket session")

        stompSession.disconnect()
        assertTrue(wsSession.closed, "disconnect() should close the web socket session")
    }

    @Test
    fun subscription_collectorCancellationUnsubscribes() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            val subFrame = wsSession.awaitSubscribeFrameAndSimulateCompletion()
            val job = launch {
                repeat(15) {
                    wsSession.simulateMessageFrameReceived(subFrame.headers.id, "MSG_$it")
                    delay(1000)
                }
            }
            wsSession.awaitUnsubscribeFrameAndSimulateCompletion(subFrame.headers.id)
            job.cancel()
            wsSession.awaitDisconnectFrameAndSimulateCompletion()
            wsSession.expectClose()
        }
        val messagesFlow = stompSession.subscribeText("/dest")

        val messages = mutableListOf<String>()
        val collectingJob = launch {
            messagesFlow.collect {
                messages.add(it)
            }
        }
        advanceTimeBy(2000)
        runCurrent()
        collectingJob.cancelAndJoin() // joining actually waits for UNSUBSCRIBE

        assertEquals(listOf("MSG_0", "MSG_1", "MSG_2"), messages)
        assertFalse(wsSession.closed, "Unsubscribe should not close the web socket session")

        stompSession.disconnect()
        assertTrue(wsSession.closed, "disconnect() should close the web socket session")
    }

    @Test
    fun subscription_disconnectDoesntNeedToUnsubscribe() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            val subFrame = wsSession.awaitSubscribeFrameAndSimulateCompletion()
            val job = launch {
                repeat(15) {
                    delay(1000)
                    wsSession.simulateMessageFrameReceived(subFrame.headers.id, "MSG_$it")
                }
            }
            val disconnectFrame = wsSession.awaitDisconnectFrameAndSimulateCompletion()
            job.cancel()
            wsSession.simulateReceiptFrameReceived(disconnectFrame.headers.receipt!!)
            // after disconnecting, we should not attempt to send an UNSUBSCRIBE frame
            wsSession.expectClose()
        }
        val messagesFlow = stompSession.subscribeText("/dest")

        val messages = mutableListOf<String>()
        val collectingJob = launch {
            messagesFlow.collect {
                messages.add(it)
            }
        }
        advanceTimeBy(3000)
        runCurrent()
        stompSession.disconnect()
        assertEquals(listOf("MSG_0", "MSG_1", "MSG_2"), messages)
        assertTrue(wsSession.closed, "disconnect() should close the web socket session")
        collectingJob.join()
        assertTrue(collectingJob.isCompleted, "The collector's job should be completed after disconnect")
        assertFalse(collectingJob.isCancelled, "The collector's job should be completed normally, not cancelled")
    }

    @Test
    fun subscription_stompErrorFrame_shouldGiveExceptionInCollector() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        val errorMessage = "some error message"
        launch {
            wsSession.awaitSubscribeFrameAndSimulateCompletion()
            wsSession.simulateErrorFrameReceived(errorMessage)
            // after receiving a STOMP error frame, we should not attempt to send an UNSUBSCRIBE or DISCONNECT frame
            wsSession.expectClose()
        }

        val messages = stompSession.subscribeText("/dest")
        val exception = assertFailsWith(StompErrorFrameReceived::class) {
            messages.first()
        }
        assertEquals(errorMessage, exception.message, "The exception in collectors should have the STOMP ERROR frame's body as message")
        assertTrue(wsSession.closed, "The web socket should be closed after a STOMP ERROR frame")
    }

    @Test
    fun subscription_webSocketError_shouldGiveExceptionInCollector() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        val errorMessage = "some error message"
        launch {
            wsSession.awaitSubscribeFrameAndSimulateCompletion()
            wsSession.simulateError(errorMessage)
            // after a web socket error, we should not attempt to send an UNSUBSCRIBE or DISCONNECT frame
            // we should also not try to close the web socket after an error
            wsSession.expectNoClose()
        }

        val messages = stompSession.subscribeText("/dest")
        val exception = assertFailsWith(WebSocketException::class) {
            messages.first()
        }
        assertEquals(errorMessage, exception.message, "The exception in collectors should have the web socket error frame's body as message")
    }

    @Test
    fun subscription_webSocketClose_shouldGiveExceptionInCollector() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            wsSession.awaitSubscribeFrameAndSimulateCompletion()
            wsSession.simulateClose(WebSocketCloseCodes.NORMAL_CLOSURE, "some reason")
            // after a web socket closure, we should not attempt to send an UNSUBSCRIBE or DISCONNECT frame
            // the web socket is already closed so it should not be attempted to close it
            wsSession.expectNoClose()
        }

        val sub = stompSession.subscribeText("/dest")
        val exception = assertFailsWith(WebSocketClosedUnexpectedly::class) {
            sub.first()
        }
        assertEquals(WebSocketCloseCodes.NORMAL_CLOSURE, exception.code)
        assertEquals("some reason", exception.reason)
    }

    @Test
    fun subscription_frameDecodingError_shouldGiveExceptionInCollector() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            wsSession.awaitSubscribeFrameAndSimulateCompletion()
            wsSession.simulateTextFrameReceived("not a valid STOMP frame")
            // after an invalid STOMP frame, we should not attempt to send an UNSUBSCRIBE or DISCONNECT frame
            wsSession.expectClose()
        }

        val sub = stompSession.subscribeText("/dest")
        assertFailsWith(InvalidStompFrameException::class) {
            sub.first()
        }
        assertTrue(wsSession.closed, "The web socket should be closed after an invalid STOMP frame is received")
    }
}
