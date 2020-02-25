package org.hildan.krossbow.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun <T> runAsyncTest(block: suspend CoroutineScope.() -> T) {
    runBlocking { block() }
}
