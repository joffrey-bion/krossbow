package org.hildan.krossbow.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.fail

fun <T> runAsyncTestWithTimeout(millis: Long = 1000, block: suspend CoroutineScope.() -> T) = runAsyncTest {
    val result = withTimeoutOrNull(millis) {
        block()
        Unit
    }
    assertNotNull(result, "this test should run in less than ${millis}ms")
}

expect fun <T> runAsyncTest(block: suspend CoroutineScope.() -> T)

expect fun getCause(exception: Exception): Throwable?

suspend fun <T> assertCompletesSoon(deferred: Deferred<T>, message: String, timeoutMillis: Long = 20): T =
        try {
            withTimeout(timeoutMillis) { deferred.await() }
        } catch (e: TimeoutCancellationException) {
            fail(message)
        }

suspend inline fun <T : Throwable> assertTimesOutWith(
    expectedExceptionClass: KClass<T>,
    expectedTimeoutMillis: Long,
    message: String = "expected time out under ${expectedTimeoutMillis}ms, with ${expectedExceptionClass.simpleName}",
    timeoutMarginMillis: Long = 30,
    crossinline block: suspend () -> Unit
): T {
    val result = withTimeoutOrNull(expectedTimeoutMillis + timeoutMarginMillis) {
        assertFailsWith(expectedExceptionClass) {
            block()
        }
    }
    assertNotNull(result, message)
    return result
}
