package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.*
import kotlin.test.Ignore
import kotlin.test.fail

actual typealias IgnoreOnJS = Ignore

@OptIn(DelicateCoroutinesApi::class)
actual fun runSuspendingTest(timeoutMillis: Long, block: suspend CoroutineScope.() -> Unit): dynamic =
    GlobalScope.promise {
        try {
            // JS tests immediately timeout if we use withTimeoutOrNull here...
            withTimeout(timeoutMillis) {
                block()
            }
        } catch (e: TimeoutCancellationException) {
            fail("Test timed out after ${timeoutMillis}ms")
        }
    }
