package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope

expect fun getDefaultAutobahnTestServerHost(): String

expect fun getDefaultAutobahnTestServerPort(): Int

expect fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit)

internal expect suspend fun runAlongEchoWSServer(block: suspend (port: Int) -> Unit)
