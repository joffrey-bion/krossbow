package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.*
import kotlin.test.Ignore
import kotlin.test.fail
import kotlin.time.*

actual typealias IgnoreOnJS = Ignore

@OptIn(DelicateCoroutinesApi::class)
actual fun runSuspendingTest(timeout: Duration, block: suspend CoroutineScope.() -> Unit): dynamic =
    GlobalScope.promise {
        try {
            // JS tests immediately timeout if we use withTimeoutOrNull here...
            withTimeout(timeout) {
                block()
            }
        } catch (e: TimeoutCancellationException) {
            fail("Test timed out after $timeout")
        }
    }
