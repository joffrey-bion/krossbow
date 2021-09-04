package org.hildan.krossbow.websocket.test

import kotlinx.cinterop.toKString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import platform.posix.getenv

actual fun getDefaultAutobahnTestServerHost(): String =
    getenv("AUTOBAHN_SERVER_HOST")?.toKString() ?: error("Environment variable AUTOBAHN_SERVER_HOST not provided")

actual fun getDefaultAutobahnTestServerPort(): Int =
    getenv("AUTOBAHN_SERVER_TCP_9001")?.toKString()?.toInt() ?: error("Env var AUTOBAHN_SERVER_TCP_9001 not provided")

actual fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit) = runBlocking { block() }

internal actual suspend fun runAlongEchoWSServer(block: suspend (port: Int) -> Unit) {
    TODO("Implement test WS echo server on native platform")
}
