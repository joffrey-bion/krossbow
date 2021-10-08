package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.*
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(DelicateCoroutinesApi::class, ExperimentalTime::class)
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

fun isBrowser() = js("typeof window !== 'undefined' && typeof window.document !== 'undefined'") as Boolean
