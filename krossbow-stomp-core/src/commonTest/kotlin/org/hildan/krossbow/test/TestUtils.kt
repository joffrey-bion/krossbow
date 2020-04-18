package org.hildan.krossbow.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.fail

fun <T> runAsyncTestWithTimeout(millis: Long = 4000, block: suspend CoroutineScope.() -> T) = runAsyncTest {
    val result = withTimeoutOrNull(millis) {
        block()
        Unit
    }
    assertNotNull(result, "this test should run in less than ${millis}ms")
}

expect fun <T> runAsyncTest(block: suspend CoroutineScope.() -> T)

expect fun getCause(exception: Exception): Throwable?

suspend fun <T> assertCompletesSoon(deferred: Deferred<T>, message: String, timeoutMillis: Long = 100): T {
    return withTimeoutOrNull(timeoutMillis) { deferred.await() } ?: fail(message)
}

suspend fun <T> CoroutineScope.assertCompletesSoon(
    message: String,
    timeoutMillis: Long = 200,
    block: suspend CoroutineScope.() -> T
): T {
    val scope = this
    val deferred = scope.async { block() }
    return assertCompletesSoon(deferred, message, timeoutMillis)
}

suspend inline fun <T : Throwable> assertTimesOutWith(
    expectedExceptionClass: KClass<T>,
    expectedTimeoutMillis: Long,
    timeoutMarginMillis: Long = 200,
    crossinline block: suspend () -> Unit
): T {
    val message = "expected time out under ${expectedTimeoutMillis}ms, with ${expectedExceptionClass.simpleName}"
    val result = assertFailsWith(expectedExceptionClass, message) {
        withTimeoutOrNull(expectedTimeoutMillis + timeoutMarginMillis) {
            block()
        }
    }
    assertNotNull(result, message)
    return result
}
