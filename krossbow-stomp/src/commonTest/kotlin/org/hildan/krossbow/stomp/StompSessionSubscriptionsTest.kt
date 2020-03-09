package org.hildan.krossbow.stomp

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.test.connectWithMocks
import org.hildan.krossbow.test.runAsyncTestWithTimeout
import org.hildan.krossbow.test.simulateErrorFrameReceived
import org.hildan.krossbow.test.simulateMessageFrameReceived
import org.hildan.krossbow.test.waitForSendAndSimulateCompletion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

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
    fun receiveSubMessage_errorFrame_shouldGiveExceptionInChannel() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks()
        val deferredSub = async { stompSession.subscribeText("/dest") }
        wsSession.waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
        val sub = deferredSub.await()

        val errorMessage = "some error message"
        launch {
            try {
                wsSession.simulateErrorFrameReceived(errorMessage)
            } catch (e: Exception) {
                fail("Listener call should succeed")
            }
        }
        val exception = assertFailsWith(StompErrorFrameReceived::class) {
            sub.messages.receive()
        }
        assertEquals(errorMessage, exception.frame.message)
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
        assertEquals(errorMessage, exception.message)
    }
}
