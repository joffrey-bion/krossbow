package org.hildan.krossbow.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.hildan.krossbow.websocket.KWebSocketClient
import org.hildan.krossbow.websocket.KWebSocketListener
import org.hildan.krossbow.websocket.KWebSocketSession
import org.hildan.krossbow.websocket.NoopWebSocketListener
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun <T> runAsyncTestWithTimeout(millis: Long = 1000, block: suspend CoroutineScope.() -> T) = runAsyncTest {
    withTimeout(millis) {
        block()
    }
}

expect fun <T> runAsyncTest(block: suspend CoroutineScope.() -> T)

class ManuallyConnectingWebSocketClient : KWebSocketClient {

    private var sessionContinuation: Continuation<MockWebSocketSession>? = null

    override suspend fun connect(url: String): KWebSocketSession = suspendCancellableCoroutine { cont ->
        sessionContinuation = cont
    }

    fun simulateConnectionSuccess(session: MockWebSocketSession) {
        getPendingContinuation().resume(session)
    }

    fun simulateConnectionFailure(exception: Throwable = Exception("connection failed (simulated)")) {
        getPendingContinuation().resumeWithException(exception)
    }

    private fun getPendingContinuation() =
            sessionContinuation ?: error("No connect() call has been issued, cannot simulate connection success")
}

class ImmediatelySucceedingWebSocketClient(
    private val session: MockWebSocketSession = MockWebSocketSession()
) : KWebSocketClient {

    override suspend fun connect(url: String): KWebSocketSession = suspendCancellableCoroutine { cont ->
        cont.resume(session)
    }
}

class ImmediatelyFailingWebSocketClient(private val exception: Throwable) : KWebSocketClient {

    override suspend fun connect(url: String): KWebSocketSession = suspendCancellableCoroutine { cont ->
        cont.resumeWithException(exception)
    }
}

class MockWebSocketSession : KWebSocketSession {

    override var listener: KWebSocketListener = NoopWebSocketListener

    var lastSentText: String? = null
    var lastSentBytes: ByteArray? = null
    var closed = false

    override suspend fun sendText(frameText: String) {
        lastSentText = frameText
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        lastSentBytes = frameData
    }

    override suspend fun close() {
        closed = true
    }
}
