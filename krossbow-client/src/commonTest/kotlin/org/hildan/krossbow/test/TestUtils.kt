package org.hildan.krossbow.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeout

fun <T> runAsyncTestWithTimeout(millis: Long = 1000, block: suspend CoroutineScope.() -> T) = runAsyncTest {
    withTimeout(millis) {
        block()
    }
}

expect fun <T> runAsyncTest(block: suspend CoroutineScope.() -> T)
