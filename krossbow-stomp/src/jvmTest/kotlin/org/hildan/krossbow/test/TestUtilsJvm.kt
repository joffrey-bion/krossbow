package org.hildan.krossbow.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun <T> runAsyncTest(block: suspend CoroutineScope.() -> T) {
    runBlocking { block() }
}

// for some reason, the cause is nested 2 levels down on JVM
actual fun getCause(exception: Exception): Throwable? = exception.cause?.cause
