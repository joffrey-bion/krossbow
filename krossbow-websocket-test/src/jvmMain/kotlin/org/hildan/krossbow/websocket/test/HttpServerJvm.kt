package org.hildan.krossbow.websocket.test

import com.sun.net.httpserver.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.net.InetSocketAddress

internal actual suspend fun runAlongHttpServer(block: suspend (baseUrl: String) -> Unit) {
    val server = HttpServer.create(InetSocketAddress("localhost", 0), 0)

    server.createContext("/failHandshakeWithStatusCode") { exch ->
        val statusCodeToRespond = exch.lastPathSegment().toInt()
        exch.sendResponseHeaders(statusCodeToRespond, -1)
    }

    server.start()
    val port = server.awaitPort()

    try {
        block("ws://localhost:$port")
    } finally {
        // stop with a delay>0 doesn't stop immediately if there is no current request (it waits the full delay)
        server.stop(0)
    }
}

private fun HttpExchange.lastPathSegment() = requestURI.path.substringAfterLast('/')

private suspend fun HttpServer.awaitPort(timeoutMillis: Long = 2000): Int = withTimeout(timeoutMillis) {
    while (address.port <= 0) {
        delay(10)
    }
    address.port
}
