package org.hildan.krossbow.websocket.jdk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.hildan.krossbow.websocket.NoopWebSocketListener
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketListener
import org.hildan.krossbow.websocket.WebSocketSession
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.nio.ByteBuffer
import java.util.concurrent.CompletionStage
import kotlin.coroutines.CoroutineContext

class Jdk11WebSocketClient(
    private val webSocketBuilder: WebSocket.Builder = HttpClient.newHttpClient().newWebSocketBuilder(),
    configure: WebSocket.Builder.() -> Unit = {}
) : WebSocketClient {

    init {
        webSocketBuilder.configure()
    }

    override suspend fun connect(url: String): WebSocketSession {
        val jdk11WebSocketListener = Jdk11WebSocketListener()
        webSocketBuilder.buildAsync(URI(url), jdk11WebSocketListener).await()
        return jdk11WebSocketListener.session
    }
}

class Jdk11WebSocketListener : WebSocket.Listener, CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job

    lateinit var session: WebSocketSession
        private set

    override fun onOpen(webSocket: WebSocket) {
        session = Jdk11WebSocketSession(webSocket, this::stop)
    }

    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> =
            async { session.listener.onTextMessage(data.toString()) }.asCompletableFuture()

    override fun onBinary(webSocket: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage<*> =
            async { session.listener.onBinaryMessage(data.array()) }.asCompletableFuture()

    override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String?): CompletionStage<*> =
            async { session.listener.onClose(statusCode, reason) }.asCompletableFuture()

    override fun onError(webSocket: WebSocket, error: Throwable?) {
        launch {
            session.listener.onError(error ?: RuntimeException("onError called without exception"))
        }
    }

    private suspend fun stop() {
        job.cancelAndJoin()
    }
}

class Jdk11WebSocketSession(
    private val webSocket: WebSocket,
    private val stopListener: suspend () -> Unit
) : WebSocketSession {

    override var listener: WebSocketListener = NoopWebSocketListener

    override suspend fun sendText(frameText: String) {
        webSocket.sendText(frameText, true).await()
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        webSocket.sendBinary(ByteBuffer.wrap(frameData), true).await()
    }

    override suspend fun close(code: Int, reason: String?) {
        webSocket.sendClose(code, reason ?: "").await()
        stopListener.invoke()
    }
}
