package org.hildan.krossbow.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

actual fun <T> runAsyncTest(context: CoroutineContext, block: suspend CoroutineScope.() -> T) {
    runBlocking(context) { block() }
}

// for some reason, the cause is nested 2 levels down on JVM
actual fun getCause(exception: Exception): Throwable? = exception.cause?.cause
