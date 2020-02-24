package org.hildan.krossbow.testutils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun <T> runAsyncTest(block: suspend CoroutineScope.() -> T): dynamic = GlobalScope.promise { block() }
