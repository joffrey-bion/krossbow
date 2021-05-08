package org.hildan.krossbow.stomp

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.test.*
import kotlin.test.*

class StompSessionExtensionsTests {

    @Test
    fun use_whenBlockTerminatedNormally_shouldDisconnectAndClose() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            wsSession.waitForSendAndSimulateCompletion()
            wsSession.waitForDisconnectAndSimulateCompletion()
            wsSession.expectClose()
        }
        stompSession.use {
            it.sendText("/dest", "Hello")
            assertFalse(wsSession.closed, "The web socket session should not be closed until the end of the use block")
        }
        assertTrue(wsSession.closed, "The web socket session should not be closed after the use block")
    }

    @Test
    fun use_whenBlockThrows_shouldThrowButStillDisconnectAndClose() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            wsSession.waitForDisconnectAndSimulateCompletion()
            wsSession.expectClose()
        }
        assertFailsWith(NoSuchElementException::class) {
            stompSession.use {
                emptyList<Int>().first() // this fails
            }
        }
        assertTrue(wsSession.closed, "The web socket session should not be closed after the use block")
    }

    @Test
    fun withTransaction_commitsIfSuccessful() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            stompSession.withTransaction { id ->
                sendText("/dest", "Transaction: $id")
            }
            stompSession.disconnect()
        }
        val beginFrame = wsSession.waitForBeginAndSimulateCompletion()
        val transactionId = beginFrame.headers.transaction

        val sendFrame = wsSession.waitForSendAndSimulateCompletion()
        assertEquals(transactionId, sendFrame.headers.transaction)

        val commitFrame = wsSession.waitForCommitAndSimulateCompletion()
        assertEquals(transactionId, commitFrame.headers.transaction)

        wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
        wsSession.expectClose()
    }

    @Test
    fun withTransaction_abortsInCaseOfException() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            runCatching {
                stompSession.withTransaction { id ->
                    sendText("/dest", "Transaction: $id")
                    emptyList<Int>().first() // this fails
                }
            }
            stompSession.disconnect()
        }
        val beginFrame = wsSession.waitForBeginAndSimulateCompletion()
        val transactionId = beginFrame.headers.transaction

        val sendFrame = wsSession.waitForSendAndSimulateCompletion()
        assertEquals(transactionId, sendFrame.headers.transaction)
        assertEquals("Transaction: $transactionId", sendFrame.bodyAsText)

        val abortFrame = wsSession.waitForAbortAndSimulateCompletion()
        assertEquals(transactionId, abortFrame.headers.transaction)

        wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
        wsSession.expectClose()
    }
}