package org.hildan.krossbow.testutils

import kotlinx.coroutines.runBlocking

actual fun <T> runAsyncTest(block: suspend () -> T) {
    runBlocking { block() }
}
