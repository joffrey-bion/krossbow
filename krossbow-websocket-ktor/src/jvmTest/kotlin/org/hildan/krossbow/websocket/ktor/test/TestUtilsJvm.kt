package org.hildan.krossbow.websocket.ktor.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit) {
    runBlocking { block() }
}
