package org.hildan.krossbow.stomp

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.frame.InvalidStompFrameException
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.test.connectWithMocks
import org.hildan.krossbow.test.getCause
import org.hildan.krossbow.test.runAsyncTestWithTimeout
import org.hildan.krossbow.test.simulateErrorFrameReceived
import org.hildan.krossbow.test.simulateMessageFrameReceived
import org.hildan.krossbow.test.waitForSendAndSimulateCompletion
import org.hildan.krossbow.websocket.WebSocketCloseCodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class StompSessionSubscriptionsTest {

    @Test
    fun receiveSubMessage_success() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()
        val deferredSub = async { stompSession.subscribeText("/dest") }
        wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
        val sub = deferredSub.await()

        launch {
            wsSession.simulateMessageFrameReceived(sub.id, "HELLO")
        }
        val message = sub.messages.receive()
        assertEquals("HELLO", message)
    }

    @Test
    fun receiveSubMessage_stompErrorFrame_shouldGiveExceptionInChannel() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()

        val errorMessage = "some error message"
        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
            wsSession.simulateErrorFrameReceived(errorMessage)
        }

        val sub = stompSession.subscribeText("/dest")
        val exception = assertFailsWith(StompErrorFrameReceived::class) {
            sub.messages.receive()
        }
        assertEquals(errorMessage, exception.frame.message)
    }

    @Test
    fun receiveSubMessage_webSocketError_shouldGiveExceptionInChannel() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()

        val errorMessage = "some error message"
        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
            wsSession.simulateError(errorMessage)
            wsSession.simulateClose(WebSocketCloseCodes.SERVER_ERROR, errorMessage)
        }

        val sub = stompSession.subscribeText("/dest")
        val exception = assertFailsWith(WebSocketError::class) {
            sub.messages.receive()
        }
        assertEquals(errorMessage, exception.message)
    }

    @Test
    fun receiveSubMessage_webSocketClose_shouldGiveExceptionInChannel() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
            wsSession.simulateClose(WebSocketCloseCodes.NORMAL_CLOSURE, "some reason")
        }

        val sub = stompSession.subscribeText("/dest")
        val exception = assertFailsWith(WebSocketClosedUnexpectedly::class) {
            sub.messages.receive()
        }
        assertEquals(WebSocketCloseCodes.NORMAL_CLOSURE, exception.code)
        assertEquals("some reason", exception.reason)
    }

    @Test
    fun receiveSubMessage_frameDecodingError_shouldGiveExceptionInChannel() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
            wsSession.simulateTextFrameReceived("not a valid STOMP frame")
        }

        val sub = stompSession.subscribeText("/dest")
        assertFailsWith(InvalidStompFrameException::class) {
            sub.messages.receive()
        }
    }

    @Test
    fun receiveSubMessage_conversionError_shouldGiveExceptionInChannel() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()

        val errorMessage = "some error message"
        val deferredSub = async {
            stompSession.subscribe<String>("/dest") { throw RuntimeException(errorMessage) }
        }
        wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
        val sub = deferredSub.await()

        launch {
            wsSession.simulateMessageFrameReceived(sub.id, "HELLO")
        }
        val exception = assertFailsWith(MessageConversionException::class) {
            sub.messages.receive()
        }
        val cause = getCause(exception)
        assertNotNull(cause)
        assertEquals(RuntimeException::class, cause::class)
        assertEquals(errorMessage, cause.message)
    }
}
