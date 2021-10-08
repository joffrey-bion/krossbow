package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
actual fun runSuspendingTest(timeout: Duration, block: suspend CoroutineScope.() -> Unit) = runBlocking {
    withTimeoutOrNull(timeout) {
        block()
    } ?: fail("Test timed out after $timeout")
}
