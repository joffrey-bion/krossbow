package org.hildan.krossbow.websocket.test

import kotlinx.cinterop.toKString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import platform.posix.getenv

actual fun getDefaultAutobahnTestServerHost(): String? = getenv("AUTOBAHN_SERVER_HOST")?.toKString()

actual fun getDefaultAutobahnTestServerPort(): Int? = getenv("AUTOBAHN_SERVER_TCP_9001")?.toKString()?.toInt()

actual fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit) = runBlocking { block() }

internal actual suspend fun runAlongEchoWSServer(block: suspend (port: Int) -> Unit) {
    TODO("Implement test WS echo server on native platform")
}
