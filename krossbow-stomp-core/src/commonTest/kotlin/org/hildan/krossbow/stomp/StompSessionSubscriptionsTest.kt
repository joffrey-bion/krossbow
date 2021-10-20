package org.hildan.krossbow.stomp

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.hildan.krossbow.stomp.frame.InvalidStompFrameException
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.headers.AckMode
import org.hildan.krossbow.stomp.headers.HeaderNames
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.test.*
import org.hildan.krossbow.websocket.WebSocketCloseCodes
import org.hildan.krossbow.websocket.WebSocketException
import kotlin.test.*

class StompSessionSubscriptionsTest {

    @Test
    fun subscribe_suspendsUntilSubscribeFrameIsSent() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        val deferredFlow = async(start = CoroutineStart.UNDISPATCHED) { stompSession.subscribeText("/dest") }
        delay(10)
        assertFalse(deferredFlow.isCompleted, "subscribe() should not return until SUBSCRIBE frame is sent")
        wsSession.waitForSubscribeAndSimulateCompletion()
        assertTrue(deferredFlow.isCompleted, "subscribe() should return when SUBSCRIBE frame is sent")

        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
            wsSession.expectClose()
        }
        stompSession.disconnect()
    }

    @Test
    fun subscribeWithReceipt_suspendsUntilReceiptIsReceived() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        val receiptId = "my-receipt"
        val deferredFlow = async(start = CoroutineStart.UNDISPATCHED) {
            val headers = StompSubscribeHeaders(destination = "/dest", receipt = receiptId)
            stompSession.subscribe(headers)
        }
        delay(10)
        assertFalse(deferredFlow.isCompleted, "subscribe() should not return until SUBSCRIBE frame is sent")
        wsSession.waitForSubscribeAndSimulateCompletion()
        delay(10)
        assertFalse(deferredFlow.isCompleted, "subscribe() should not return until the RECEIPT is received")
        wsSession.simulateReceiptFrameReceived("not-my-receipt")
        delay(10)
        assertFalse(deferredFlow.isCompleted, "subscribe() should not return until the correct RECEIPT is received")
        wsSession.simulateReceiptFrameReceived(receiptId)
        assertTrue(deferredFlow.isCompleted, "subscribe() call should return when SUBSCRIBE frame is sent")

        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
            wsSession.expectClose()
        }
        stompSession.disconnect()
    }

    @Test
    fun subscribe_doesntLoseMessagesIfFlowIsNotCollectedImmediately() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        val subFrame = async { wsSession.waitForSubscribeAndSimulateCompletion() }
        val messages = stompSession.subscribeText("/dest")

        val subId = subFrame.await().headers.id
        wsSession.simulateMessageFrameReceived(subId, "HELLO")

        launch {
            wsSession.waitForUnsubscribeAndSimulateCompletion(subId)
            wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
            wsSession.expectClose()
        }

        delay(50) // leave some time for the frame to potentially be lost
        val message = messages.first()
        assertEquals("HELLO", message)
        stompSession.disconnect()
    }

    @Test
    fun subscribeSendCollect_shouldNotLoseMessages() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
            val subId = subFrame.headers.id

            // we simulate that a SEND frame triggers a MESSAGE frame on the subscription
            val sendFrame = wsSession.waitForSendAndSimulateCompletion()
            wsSession.simulateMessageFrameReceived(subId, sendFrame.bodyAsText)

            wsSession.waitForUnsubscribeAndSimulateCompletion(subId)
            wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
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
    fun subscribe_headersAreRespected() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        val destination = "/dest"
        val customId = "some_id"
        val customAck = AckMode.CLIENT
        val customHeaderKey = "my-header"
        val customHeaderValue = "my-header-value"
        launch {
            val headers = StompSubscribeHeaders(
                destination = destination,
                id = customId,
                ack = customAck,
                customHeaders = mapOf(customHeaderKey to customHeaderValue)
            )
            stompSession.subscribe(headers).first()
            stompSession.disconnect()
        }
        val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
        assertEquals(destination, subFrame.headers.destination)
        assertEquals(customId, subFrame.headers.id)
        assertEquals(customAck, subFrame.headers.ack)
        assertEquals(customHeaderValue, subFrame.headers[customHeaderKey])
        wsSession.simulateMessageFrameReceived(subFrame.headers.id, "HELLO")
        wsSession.waitForUnsubscribeAndSimulateCompletion(subFrame.headers.id)
        wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
        wsSession.expectClose()
    }

    @Test
    fun subscribe_headersAreRespectedEvenIfIdIsAutoGenerated() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        val destination = "/dest"
        val customAck = AckMode.CLIENT
        val customHeaderKey = "my-header"
        val customHeaderValue = "my-header-value"

        val headers = StompSubscribeHeaders(
            destination = destination,
            ack = customAck,
            customHeaders = mapOf(customHeaderKey to customHeaderValue)
        )

        launch {
            stompSession.subscribe(headers).first()
            stompSession.disconnect()
        }

        val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
        assertTrue(subFrame.headers.id.isNotBlank(), "an ID should be generated")
        assertEquals(destination, subFrame.headers.destination)
        assertEquals(customAck, subFrame.headers.ack)
        assertEquals(customHeaderValue, subFrame.headers[customHeaderKey])
        assertNull(headers[HeaderNames.ID], "The original headers (without ID) should not be modified.")

        wsSession.simulateMessageFrameReceived(subFrame.headers.id, "HELLO")
        wsSession.waitForUnsubscribeAndSimulateCompletion(subFrame.headers.id)
        wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
        wsSession.expectClose()
    }

    @Test
    fun subscription_firstOperatorUnsubscribes() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
            wsSession.simulateMessageFrameReceived(subFrame.headers.id, "HELLO")
            wsSession.waitForUnsubscribeAndSimulateCompletion(subFrame.headers.id)
            wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
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
    fun subscription_takeOperatorUnsubscribes() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
            repeat(3) {
                wsSession.simulateMessageFrameReceived(subFrame.headers.id, "MSG_$it")
            }
            wsSession.waitForUnsubscribeAndSimulateCompletion(subFrame.headers.id)
            wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
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
    fun subscription_collectorCancellationUnsubscribes() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
            val job = launch {
                repeat(15) {
                    wsSession.simulateMessageFrameReceived(subFrame.headers.id, "MSG_$it")
                    delay(1000)
                }
            }
            wsSession.waitForUnsubscribeAndSimulateCompletion(subFrame.headers.id)
            job.cancel()
            wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
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
        collectingJob.cancelAndJoin() // joining actually waits for UNSUBSCRIBE

        assertEquals(listOf("MSG_0", "MSG_1", "MSG_2"), messages)
        assertFalse(wsSession.closed, "Unsubscribe should not close the web socket session")

        stompSession.disconnect()
        assertTrue(wsSession.closed, "disconnect() should close the web socket session")
    }

    @Test
    fun subscription_disconnectDoesntNeedToUnsubscribe() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
            val job = launch {
                repeat(15) {
                    delay(1000)
                    wsSession.simulateMessageFrameReceived(subFrame.headers.id, "MSG_$it")
                }
            }
            val disconnectFrame = wsSession.waitForDisconnectAndSimulateCompletion()
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
        stompSession.disconnect()
        assertEquals(listOf("MSG_0", "MSG_1", "MSG_2"), messages)
        assertTrue(wsSession.closed, "disconnect() should close the web socket session")
        collectingJob.join()
        assertTrue(collectingJob.isCompleted, "The collector's job should be completed after disconnect")
        assertFalse(collectingJob.isCancelled, "The collector's job should be completed normally, not cancelled")
    }

    @Test
    fun subscription_stompErrorFrame_shouldGiveExceptionInCollector() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        val errorMessage = "some error message"
        launch {
            wsSession.waitForSubscribeAndSimulateCompletion()
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
    fun subscription_webSocketError_shouldGiveExceptionInCollector() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        val errorMessage = "some error message"
        launch {
            wsSession.waitForSubscribeAndSimulateCompletion()
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
    fun subscription_webSocketClose_shouldGiveExceptionInCollector() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            wsSession.waitForSubscribeAndSimulateCompletion()
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
    fun subscription_frameDecodingError_shouldGiveExceptionInCollector() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            wsSession.waitForSubscribeAndSimulateCompletion()
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
