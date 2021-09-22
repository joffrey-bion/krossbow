package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope

expect fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit)
