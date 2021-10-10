package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.fail
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
actual fun runSuspendingTest(timeoutMillis: Long, block: suspend CoroutineScope.() -> Unit) = runBlocking {
    withTimeoutOrNull(timeoutMillis) {
        block()
    } ?: fail("Test timed out after ${timeoutMillis}ms")
}
