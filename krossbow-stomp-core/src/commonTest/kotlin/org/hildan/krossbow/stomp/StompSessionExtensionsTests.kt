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

    @Test
    fun withTransaction_commitsIfSuccessful() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            stompSession.withTransaction { id ->
                sendText("/dest", "Transaction: $id")
            }
            stompSession.disconnect()
        }
        val beginFrame = wsSession.awaitBeginFrameAndSimulateCompletion()
        val transactionId = beginFrame.headers.transaction

        val sendFrame = wsSession.awaitSendFrameAndSimulateCompletion()
        assertEquals(transactionId, sendFrame.headers.transaction)

        val commitFrame = wsSession.awaitCommitFrameAndSimulateCompletion()
        assertEquals(transactionId, commitFrame.headers.transaction)

        wsSession.awaitDisconnectFrameAndSimulateCompletion()
        wsSession.expectClose()
    }

    @Test
    fun withTransaction_abortsInCaseOfException() = runTest {
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
        val beginFrame = wsSession.awaitBeginFrameAndSimulateCompletion()
        val transactionId = beginFrame.headers.transaction

        val sendFrame = wsSession.awaitSendFrameAndSimulateCompletion()
        assertEquals(transactionId, sendFrame.headers.transaction)
        assertEquals("Transaction: $transactionId", sendFrame.bodyAsText)

        val abortFrame = wsSession.awaitAbortFrameAndSimulateCompletion()
        assertEquals(transactionId, abortFrame.headers.transaction)

        wsSession.awaitDisconnectFrameAndSimulateCompletion()
        wsSession.expectClose()
    }

    @Test
    fun withTransaction_abortsWithSuppressedException() = runTest {
        class MyAbortException : Exception("exception during abort")

        val stompSession = object : NoopStompSession() {
            override suspend fun abort(transactionId: String) {
                throw MyAbortException()
            }
        }

        val ex = assertFailsWith(NoSuchElementException::class) {
            stompSession.withTransaction {
                emptyList<Int>().first() // this fails
            }
        }
        assertEquals(1, ex.suppressedExceptions.size)
        assertIs<MyAbortException>(ex.suppressedExceptions.single())
    }
}
