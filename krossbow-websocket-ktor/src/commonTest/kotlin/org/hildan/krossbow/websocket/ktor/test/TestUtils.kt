package org.hildan.krossbow.websocket.ktor.test

import kotlinx.coroutines.CoroutineScope

expect fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit)
