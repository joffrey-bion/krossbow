package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.fail
import kotlin.time.*

actual fun runSuspendingTest(timeout: Duration, block: suspend CoroutineScope.() -> Unit) = runBlocking {
    withTimeoutOrNull(timeout) {
        block()
    } ?: fail("Test timed out after $timeout")
}
