package org.hildan.krossbow.websocket.ktor

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.request.port
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpStatement
import io.ktor.http.DEFAULT_PORT
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.http.takeFrom
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.hildan.krossbow.websocket.KWebSocketClient
import org.hildan.krossbow.websocket.KWebSocketListener
import org.hildan.krossbow.websocket.KWebSocketSession
import org.hildan.krossbow.websocket.NoopWebSocketListener

suspend fun HttpClient.simpleWebsocket(urlString: String): DefaultClientWebSocketSession {

//    webSocket(
//        HttpMethod.Get, "localhost", DEFAULT_PORT, "/", {
//            url.protocol = URLProtocol.WS
//            url.port = port
//
//            url.takeFrom(urlString)
//            // request() = {}
//        }, {
//            // block
//        }
//    )

//    webSocket(
//        {
//            this.method = HttpMethod.Get // method
//            url("ws", "localhost" /* host */, DEFAULT_PORT /* port */, "/" /* path */)
//            // request():
//            url.protocol = URLProtocol.WS
//            url.port = port
//
//            url.takeFrom(urlString)
//        }, {
//            // block
//        }
//    )

    val session = request<HttpStatement> {
        url {
            protocol = URLProtocol.WS
            port = protocol.defaultPort
        }
        // request():
        this.method = HttpMethod.Get // method
        url("ws", "localhost" /* host */, DEFAULT_PORT /* port */, "/" /* path */)
        // previous request():
        url.protocol = URLProtocol.WS
        url.port = port

        url.takeFrom(urlString)
    }

//    session.receive<DefaultClientWebSocketSession, Unit> {
//        try {
//            block(it)
//        } finally {
//            it.close()
//        }
//    }
    return session.receive<DefaultClientWebSocketSession, DefaultClientWebSocketSession> { it }
}

suspend fun HttpClient.explicitWebsocket(urlString: String, block: suspend DefaultClientWebSocketSession.() -> Unit) {
    val session = request<HttpStatement> {
        url {
            protocol = URLProtocol.WS
            port = protocol.defaultPort
        }
        // request():
        this.method = HttpMethod.Get // method
        url("ws", "localhost" /* host */, DEFAULT_PORT /* port */, "/" /* path */)
        // previous request():
        url.protocol = URLProtocol.WS
        url.port = port

        url.takeFrom(urlString)
    }

    session.receive<DefaultClientWebSocketSession, Unit> {
        try {
            block(it)
        } finally {
            it.close()
        }
    }
}

class KtorWebSocket : KWebSocketClient {

    @UseExperimental(KtorExperimentalAPI::class)
    private val client: HttpClient = HttpClient { install(WebSockets) }

    override suspend fun connect(url: String): KWebSocketSession {
        val wsKtorSession = client.simpleWebsocket(url)
        return KtorWebSocketSessionAdapter(wsKtorSession)
    }
}

class KtorWebSocketSessionAdapter(private val wsSession: DefaultClientWebSocketSession) : KWebSocketSession {

    override var listener: KWebSocketListener = NoopWebSocketListener

    private val job = GlobalScope.launch {
        for (frame in wsSession.incoming) {
            when (frame) {
                is Frame.Text -> listener.onTextMessage(frame.readText())
                is Frame.Binary -> listener.onBinaryMessage(frame.readBytes())
                is Frame.Close -> listener.onClose()
            }
        }
    }

    override suspend fun send(frameData: ByteArray) {
        wsSession.outgoing.send(Frame.Binary(false, frameData))
    }

    override suspend fun close() {
        wsSession.close()
        job.cancelAndJoin()
    }
}
