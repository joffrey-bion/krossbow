package org.hildan.krossbow.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.coroutines.CoroutineContext

actual fun <T> runAsyncTest(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): dynamic = GlobalScope.promise(context) { block() }

actual fun getCause(exception: Exception): Throwable? = exception.cause
