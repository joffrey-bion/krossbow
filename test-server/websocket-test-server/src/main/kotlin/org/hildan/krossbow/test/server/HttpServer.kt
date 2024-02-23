package org.hildan.krossbow.test.server

import com.sun.net.httpserver.*
import kotlinx.coroutines.*
import java.net.*

internal fun TestHttpServer(): HttpServer = HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
    createContext("/failHandshakeWithStatusCode") { exch ->
        val statusCodeToRespond = exch.lastPathSegment().toInt()
        exch.sendResponseHeaders(statusCodeToRespond, -1)
    }
}

private fun HttpExchange.lastPathSegment() = requestURI.path.substringAfterLast('/')

internal suspend fun HttpServer.startAndAwaitPort(): Int {
    start()
    return awaitPort()
}

private suspend fun HttpServer.awaitPort(timeoutMillis: Long = 2000): Int = withTimeout(timeoutMillis) {
    while (address.port <= 0) {
        delay(10)
    }
    address.port
}
