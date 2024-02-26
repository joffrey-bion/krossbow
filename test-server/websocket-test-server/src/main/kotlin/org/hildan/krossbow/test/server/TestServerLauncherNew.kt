package org.hildan.krossbow.test.server

//import io.ktor.server.application.*
//import io.ktor.server.engine.*
//import io.ktor.server.netty.*
//import io.ktor.server.request.*
//import io.ktor.server.response.*
//import io.ktor.server.routing.*
//
//suspend fun startTestServerNew(): TestServer {
//    println("Starting test WS server...")
//    val wsServer = startWebSocketServer()
//    val wsPort = wsServer.port
//    println("Test WS server listening on port $wsPort")
//
//    println("Starting test HTTP server...")
//    val httpServer = startHttpServer(WebSocketTestServer(wsServer))
//    val httpPort = httpServer.resolvedConnectors().first().port
//    println("Test HTTP server listening on port $httpPort")
//
//    return object : TestServer {
//        override val host: String = "localhost"
//        override val wsPort: Int get() = wsPort
//        override val httpPort: Int get() = httpPort
//
//        override fun stop() {
//            wsServer.stop()
//        }
//    }
//}
//
//class WebSocketTestServer(server: KWebSocketServer) {
//
//}
//
//fun startHttpServer(wsServer: WebSocketTestServer): NettyApplicationEngine {
//    return embeddedServer(
//        factory = Netty,
//        port = 0,
//        host = "0.0.0.0",
//    ) {
//        module(wsServer)
//    }.start(wait = false)
//}
//
//private fun Application.module(wsServer: WebSocketTestServer) {
//    routing {
//        get("hello") {
//            call.respondText("Hello")
//        }
//        route("connection/{connectionId}") {
//            post("sendText") {
//                val connectionId = call.connectionIdParam
//                println(connectionId)
//                val body = call.receiveText()
////                wsServer.socket(connectionId).send(body)
//            }
//            post("close") {
//                val connectionId = call.connectionIdParam
////                wsServer.socket(connectionId).close(0) // TODO
//            }
//        }
//    }
//}
//
//private val ApplicationCall.connectionIdParam
//    get() = parameters["connectionId"] ?: error("Missing connectionId parameter")
