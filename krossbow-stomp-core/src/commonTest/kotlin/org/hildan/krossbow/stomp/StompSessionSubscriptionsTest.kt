package org.hildan.krossbow.stomp

import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.hildan.krossbow.test.*
import org.hildan.krossbow.websocket.WebSocketCloseCodes
import org.hildan.krossbow.websocket.WebSocketException
import kotlin.test.*

class StompSessionSubscriptionsTest {

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

    @OptIn(ExperimentalCoroutinesApi::class)
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
