package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit) {
    GlobalScope.promise { block() }
}
