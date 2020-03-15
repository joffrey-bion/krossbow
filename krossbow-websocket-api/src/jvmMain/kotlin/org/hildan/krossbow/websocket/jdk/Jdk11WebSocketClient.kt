package org.hildan.krossbow.websocket.jdk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import org.hildan.krossbow.websocket.WebSocketListenerChannelAdapter
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnectionException
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketSession
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.nio.ByteBuffer
import java.util.concurrent.CompletionStage
import kotlin.coroutines.CoroutineContext

/**
 * A [WebSocketClient] implementation using JDK11's async web socket API.
 */
class Jdk11WebSocketClient(
    private val webSocketBuilder: WebSocket.Builder = HttpClient.newHttpClient().newWebSocketBuilder(),
    configure: WebSocket.Builder.() -> Unit = {}
) : WebSocketClient {

    init {
        webSocketBuilder.configure()
    }

    override suspend fun connect(url: String): WebSocketSession {
        try {
            val listener = WebSocketListenerChannelAdapter()
            val jdk11WebSocketListener = Jdk11WebSocketListener(listener)
            val webSocket = webSocketBuilder.buildAsync(URI(url), jdk11WebSocketListener).await()
            return Jdk11WebSocketSession(webSocket, listener.incomingFrames, jdk11WebSocketListener::stop)
        } catch (e: Exception) {
            throw WebSocketConnectionException(url, cause = e)
        }
    }
}

private class Jdk11WebSocketListener(
    private val listener: WebSocketListenerChannelAdapter
) : WebSocket.Listener, CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job

    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> {
        webSocket.request(1)
        return async { listener.onTextMessage(data.toString()) }.asCompletableFuture()
    }

    override fun onBinary(webSocket: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage<*> {
        webSocket.request(1)
        val array = ByteArray(data.remaining())
        data.get(array)
        return async { listener.onBinaryMessage(array) }.asCompletableFuture()
    }

    override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String?): CompletionStage<*> {
        return async { listener.onClose(statusCode, reason) }.asCompletableFuture()
    }

    override fun onError(webSocket: WebSocket, error: Throwable?) {
        listener.onError(error)
        job.cancel()
    }

    suspend fun stop() {
        job.cancelAndJoin()
    }
}

/**
 * An adapter wrapping JDK11's async [WebSocket] as a [WebSocketSession].
 */
class Jdk11WebSocketSession(
    private val webSocket: WebSocket,
    override val incomingFrames: ReceiveChannel<WebSocketFrame>,
    private val stopListener: suspend () -> Unit
) : WebSocketSession {

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
