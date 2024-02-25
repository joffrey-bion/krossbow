package org.hildan.krossbow.test.server

import kotlinx.coroutines.*

interface TestServer {
    val host: String
    val wsPort: Int
    val httpPort: Int

    fun stop()
}

fun startTestServer(): TestServer = runBlocking {
    println("Starting test WS server...")
    val wsServer = EchoWebSocketServer()
    val wsPort = wsServer.startAndAwaitPort()
    println("Test WS server listening on port $wsPort")

    println("Starting test HTTP server...")
    val httpServer = TestHttpServer()
    val httpPort = httpServer.startAndAwaitPort()
    println("Test HTTP server listening on port $httpPort")

    object : TestServer {
        override val host: String = "localhost"
        override val wsPort: Int get() = wsPort
        override val httpPort: Int get() = httpPort

        override fun stop() {
            println("Stopping test WS server...")
            wsServer.stop()
            println("Stopping test HTTP server...")
            // stop with a delay>0 doesn't stop immediately if there is no current request (it waits the full delay)
            httpServer.stop(0)
            println("Test servers stopped")
        }
    }
}
