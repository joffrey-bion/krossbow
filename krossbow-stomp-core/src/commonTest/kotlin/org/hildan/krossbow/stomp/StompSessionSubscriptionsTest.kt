package org.hildan.krossbow.stomp

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.frame.InvalidStompFrameException
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.test.connectWithMocks
import org.hildan.krossbow.test.runAsyncTestWithTimeout
import org.hildan.krossbow.test.simulateErrorFrameReceived
import org.hildan.krossbow.test.simulateMessageFrameReceived
import org.hildan.krossbow.test.waitForSendAndSimulateCompletion
import org.hildan.krossbow.test.waitForSubscribeAndSimulateCompletion
import org.hildan.krossbow.test.waitForUnsubscribeAndSimulateCompletion
import org.hildan.krossbow.websocket.WebSocketCloseCodes
import org.hildan.krossbow.websocket.WebSocketException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StompSessionSubscriptionsTest {

    @Test
    fun subscription_terminalOperatorUnsubscribes() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
            wsSession.simulateMessageFrameReceived(subFrame.headers.id, "HELLO")
            wsSession.waitForUnsubscribeAndSimulateCompletion(subFrame.headers.id)
            wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
        }
        val messages = stompSession.subscribeText("/dest")
        val message = messages.first()
        assertEquals("HELLO", message)
        assertFalse(wsSession.closed)

        stompSession.disconnect()
        assertTrue(wsSession.closed)
    }

    @Test
    fun subscription_collectorCancellationUnsubscribes() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
            val job = launch {
                repeat(15) {
                    wsSession.simulateMessageFrameReceived(subFrame.headers.id, "MSG_$it")
                    delay(200)
                }
            }
            wsSession.waitForUnsubscribeAndSimulateCompletion(subFrame.headers.id)
            job.cancel()
            wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
        }
        val messagesFlow = stompSession.subscribeText("/dest")

        val messages = mutableListOf<String>()
        val collectingJob = launch {
            messagesFlow.collect {
                messages.add(it)
            }
        }
        delay(500)
        collectingJob.cancelAndJoin() // joining actually waits for UNSUBSCRIBE

        assertEquals(listOf("MSG_0", "MSG_1", "MSG_2"), messages)
        assertFalse(wsSession.closed)

        stompSession.disconnect()
        assertTrue(wsSession.closed)
    }

    @Test
    fun subscription_stompErrorFrame_shouldGiveExceptionInCollector() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()

        val errorMessage = "some error message"
        launch {
            wsSession.waitForSubscribeAndSimulateCompletion()
            wsSession.simulateErrorFrameReceived(errorMessage)
            // after receiving a STOMP error frame, we should not attempt to send any STOMP frame
            // if this test hangs, we're doing something wrong (probably sending a STOMP frame)
        }

        val messages = stompSession.subscribeText("/dest")
        val exception = assertFailsWith(StompErrorFrameReceived::class) {
            messages.first()
        }
        assertEquals(errorMessage, exception.frame.message)
        assertTrue(wsSession.closed)
    }

    @Test
    fun subscription_webSocketError_shouldGiveExceptionInCollector() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()

        val errorMessage = "some error message"
        launch {
            wsSession.waitForSubscribeAndSimulateCompletion()
            wsSession.simulateError(errorMessage)
            wsSession.simulateClose(WebSocketCloseCodes.SERVER_ERROR, errorMessage)
            // after a web socket error, we should not attempt to send any STOMP frame
            // if this test hangs, we're doing something wrong (probably sending a STOMP frame)
        }

        val messages = stompSession.subscribeText("/dest")
        val exception = assertFailsWith(WebSocketException::class) {
            messages.first()
        }
        assertEquals(errorMessage, exception.message)
        assertTrue(wsSession.closed)
    }

    @Test
    fun subscription_webSocketClose_shouldGiveExceptionInCollector() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            wsSession.waitForSubscribeAndSimulateCompletion()
            wsSession.simulateClose(WebSocketCloseCodes.NORMAL_CLOSURE, "some reason")
        }

        val sub = stompSession.subscribeText("/dest")
        val exception = assertFailsWith(WebSocketClosedUnexpectedly::class) {
            sub.first()
        }
        assertEquals(WebSocketCloseCodes.NORMAL_CLOSURE, exception.code)
        assertEquals("some reason", exception.reason)
        assertTrue(wsSession.closed)
    }

    @Test
    fun subscription_frameDecodingError_shouldGiveExceptionInCollector() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            wsSession.waitForSubscribeAndSimulateCompletion()
            wsSession.simulateTextFrameReceived("not a valid STOMP frame")
        }

        val sub = stompSession.subscribeText("/dest")
        assertFailsWith(InvalidStompFrameException::class) {
            sub.first()
        }
        assertTrue(wsSession.closed)
    }
}
