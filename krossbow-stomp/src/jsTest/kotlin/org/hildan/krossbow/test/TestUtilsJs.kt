package org.hildan.krossbow.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun <T> runAsyncTest(block: suspend CoroutineScope.() -> T): dynamic = GlobalScope.promise { block() }

actual fun getCause(exception: Exception): Throwable? = exception.cause
