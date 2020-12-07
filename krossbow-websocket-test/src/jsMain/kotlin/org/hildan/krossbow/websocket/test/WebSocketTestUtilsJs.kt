package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

// FIXME this will not work on MacOS runners, we need to get info from docker compose service somehow.
//   JVM-based tests can use the environment, but native and JS will have to find another way
actual fun getDefaultAutobahnTestServerHost(): String? = "localhost"

actual fun getDefaultAutobahnTestServerPort(): Int? = 9001

@OptIn(DelicateCoroutinesApi::class)
actual fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit): dynamic = GlobalScope.promise { block() }

internal actual suspend fun runAlongEchoWSServer(block: suspend (port: Int) -> Unit) {
    TODO("Implement test WS echo server on JS platform")
}
