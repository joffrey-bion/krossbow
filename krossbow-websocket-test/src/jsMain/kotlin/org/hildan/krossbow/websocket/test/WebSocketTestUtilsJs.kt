package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

@OptIn(DelicateCoroutinesApi::class)
actual fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit): dynamic = GlobalScope.promise { block() }

// FIXME this will not work on MacOS runners, we need to get info from docker compose service somehow.
//   JVM-based tests can use the environment, but native and JS will have to find another way
actual fun getDefaultAutobahnTestServerHost(): String = when (isBrowser()) {
    true -> "localhost"
    false -> readAutobahnConfigNodeJS().host
}

actual fun getDefaultAutobahnTestServerPort(): Int = when (isBrowser()) {
    true -> 9001
    false -> readAutobahnConfigNodeJS().port
}

external interface AutobahnConfig {
    val host: String
    val port: Int
}

private fun readAutobahnConfigNodeJS(): AutobahnConfig =
    js("JSON.parse(require('fs').readFileSync('./autobahn-server.json', 'utf8'))").unsafeCast<AutobahnConfig>()

fun isBrowser() = js("typeof window !== 'undefined' && typeof window.document !== 'undefined'") as Boolean

internal actual suspend fun runAlongEchoWSServer(block: suspend (port: Int) -> Unit) {
    TODO("Implement test WS echo server on JS platform")
}
