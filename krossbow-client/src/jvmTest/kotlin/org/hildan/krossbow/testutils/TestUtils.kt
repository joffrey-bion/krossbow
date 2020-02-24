package org.hildan.krossbow.testutils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun <T> runAsyncTest(block: suspend CoroutineScope.() -> T) {
    runBlocking { block() }
}
