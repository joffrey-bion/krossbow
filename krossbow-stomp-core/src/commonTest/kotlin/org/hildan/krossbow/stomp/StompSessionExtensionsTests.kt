package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.hildan.krossbow.test.*
import kotlin.test.*

class StompSessionExtensionsTests {

    @Test
    fun use_whenBlockTerminatedNormally_shouldDisconnectAndClose() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            wsSession.awaitSendFrameAndSimulateCompletion()
            wsSession.awaitDisconnectFrameAndSimulateCompletion()
            wsSession.expectClose()
        }
        stompSession.use {
            it.sendText("/dest", "Hello")
            assertFalse(wsSession.closed, "The web socket session should not be closed until the end of the use block")
        }
        assertTrue(wsSession.closed, "The web socket session should be closed after the use block")
    }

    @Test
    fun use_whenBlockThrows_shouldThrowButStillDisconnectAndClose() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            wsSession.awaitDisconnectFrameAndSimulateCompletion()
            wsSession.expectClose()
        }

        class MyTestException : Exception()

        assertFailsWith(MyTestException::class) {
            stompSession.use {
                throw MyTestException()
            }
        }
        assertTrue(wsSession.closed, "The web socket session should be closed after the use block")
    }
}
