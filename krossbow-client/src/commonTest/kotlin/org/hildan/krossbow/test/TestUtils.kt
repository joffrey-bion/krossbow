package org.hildan.krossbow.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.assertNotNull

fun <T> runAsyncTestWithTimeout(millis: Long = 1000, block: suspend CoroutineScope.() -> T) = runAsyncTest {
    val result = withTimeoutOrNull(millis) {
        block()
        Unit
    }
    assertNotNull(result, "this test should run in less than ${millis}ms")
}

expect fun <T> runAsyncTest(block: suspend CoroutineScope.() -> T)
