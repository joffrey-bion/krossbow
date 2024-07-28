package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.hildan.krossbow.stomp.frame.*
import org.hildan.krossbow.stomp.headers.*
import org.hildan.krossbow.test.*
import kotlin.test.*

class StompSessionTransactionsTests {

    private class MyTestException : Exception("exception during transaction")

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
                    throw MyTestException()
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

        val ex = assertFailsWith(MyTestException::class) {
            stompSession.withTransaction {
                throw MyTestException()
            }
        }
        assertEquals(1, ex.suppressedExceptions.size)
        assertIs<MyAbortException>(ex.suppressedExceptions.single())
    }

    @Test
    fun withTransaction_nested() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            stompSession.withTransaction { id1 ->
                sendText("/dest", "Transaction: $id1")
                stompSession.withTransaction { id2 ->
                    sendText("/dest2", "Transaction: $id2")
                }
            }
            stompSession.disconnect()
        }
        val beginFrameT1 = wsSession.awaitBeginFrameAndSimulateCompletion()
        val transactionId1 = beginFrameT1.headers.transaction

        val sendFrameT1 = wsSession.awaitSendFrameAndSimulateCompletion()
        assertEquals(transactionId1, sendFrameT1.headers.transaction)
        assertEquals("Transaction: $transactionId1", sendFrameT1.bodyAsText)

        val beginFrameT2 = wsSession.awaitBeginFrameAndSimulateCompletion()
        val transactionId2 = beginFrameT2.headers.transaction

        val sendFrameT2 = wsSession.awaitSendFrameAndSimulateCompletion()
        assertEquals(transactionId2, sendFrameT2.headers.transaction)
        assertEquals("Transaction: $transactionId2", sendFrameT2.bodyAsText)

        val commitFrameT2 = wsSession.awaitCommitFrameAndSimulateCompletion()
        assertEquals(transactionId2, commitFrameT2.headers.transaction)

        val commitFrameT1 = wsSession.awaitCommitFrameAndSimulateCompletion()
        assertEquals(transactionId1, commitFrameT1.headers.transaction)

        wsSession.awaitDisconnectFrameAndSimulateCompletion()
        wsSession.expectClose()
    }

    @Test
    fun withTransaction_respectExistingTransactionHeader() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        launch {
            stompSession.withTransaction { id ->
                val headers = StompSendHeaders("/dest", transaction = "override")
                send(headers, FrameBody.Text("Transaction: override"))
                sendText("/dest", "Transaction: $id")
            }
            stompSession.disconnect()
        }
        val beginFrame = wsSession.awaitBeginFrameAndSimulateCompletion()
        val transactionId = beginFrame.headers.transaction

        val sendFrame = wsSession.awaitSendFrameAndSimulateCompletion()
        assertEquals("override", sendFrame.headers.transaction)
        assertEquals("Transaction: override", sendFrame.bodyAsText)

        val sendFrame2 = wsSession.awaitSendFrameAndSimulateCompletion()
        assertEquals(transactionId, sendFrame2.headers.transaction)
        assertEquals("Transaction: $transactionId", sendFrame2.bodyAsText)

        val commitFrame = wsSession.awaitCommitFrameAndSimulateCompletion()
        assertEquals(transactionId, commitFrame.headers.transaction)

        wsSession.awaitDisconnectFrameAndSimulateCompletion()
        wsSession.expectClose()
    }

    @Test
    fun withTransaction_doesNotMutateHeaders() = runTest {
        val (wsSession, stompSession) = connectWithMocks()

        val initialHeaders = StompSendHeaders("/dest")
        launch {
            stompSession.withTransaction { id ->
                send(initialHeaders, FrameBody.Text("Transaction: $id"))
            }
            stompSession.disconnect()
        }
        val beginFrame = wsSession.awaitBeginFrameAndSimulateCompletion()
        val transactionId = beginFrame.headers.transaction

        val sendFrame = wsSession.awaitSendFrameAndSimulateCompletion()
        assertEquals(transactionId, sendFrame.headers.transaction)
        assertEquals("Transaction: $transactionId", sendFrame.bodyAsText)

        val commitFrame = wsSession.awaitCommitFrameAndSimulateCompletion()
        assertEquals(transactionId, commitFrame.headers.transaction)

        assertNull(initialHeaders.transaction, "'transaction' header should not be mutated in original headers.")

        wsSession.awaitDisconnectFrameAndSimulateCompletion()
        wsSession.expectClose()
    }
}
