package org.hildan.krossbow.test

import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestCoroutineScope
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.test.fail

suspend fun <T : Throwable> TestCoroutineScope.assertTimesOutWith(
    expectedExceptionClass: KClass<T>,
    expectedTimeoutMillis: Long,
    block: suspend () -> Unit,
): T {
    val scope = this
    val deferred = scope.async {
        runCatching {
            block()
        }
    }
    advanceTimeBy(expectedTimeoutMillis - 1)
    if (!deferred.isActive) {
        val result = deferred.await()
        if (result.isFailure) {
            val ex = result.exceptionOrNull()!!
            fail(
                "expected time out with ${expectedExceptionClass.simpleName} (after ${expectedTimeoutMillis}ms) but " +
                    "another exception was thrown before that: ${ex::class.simpleName}: ${ex.message}"
            )
        } else {
            fail(
                "expected time out after ${expectedTimeoutMillis}ms (with ${expectedExceptionClass.simpleName}) but " + "the block completed successfully before that time"
            )
        }
    }
    advanceTimeBy(1)
    if (deferred.isActive) {
        fail(
            "expected time out after ${expectedTimeoutMillis}ms (with ${expectedExceptionClass.simpleName}) but " + "nothing happened (block still suspended)"
        )
    }
    val result = deferred.await()
    if (!result.isFailure) {
        fail(
            "expected time out after ${expectedTimeoutMillis}ms (with ${expectedExceptionClass.simpleName}) but " + "the block completed successfully (right on time!)"
        )
    }
    val ex = result.exceptionOrNull()!!
    if (!expectedExceptionClass.isInstance(ex)) {
        fail(
            "expected time out with ${expectedExceptionClass.simpleName} (after ${expectedTimeoutMillis}ms) but " + "an exception of type ${ex::class.simpleName} was thrown instead (at the right time)"
        )
    }
    return expectedExceptionClass.cast(ex)
}
