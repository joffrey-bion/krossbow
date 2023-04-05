package org.hildan.krossbow.websocket.jdk

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hildan.krossbow.websocket.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.net.http.WebSocketHandshakeException
import java.nio.ByteBuffer
import java.util.concurrent.CompletionStage

/**
 * A [WebSocketClient] implementation using JDK11's async web socket API.
 */
class Jdk11WebSocketClient(
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val configureWebSocket: WebSocket.Builder.() -> Unit = {}
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketConnectionWithPingPong {
        try {
            val listener = WebSocketListenerFlowAdapter()
            val jdk11WebSocketListener = Jdk11WebSocketListener(listener)
            val webSocket = httpClient.newWebSocketBuilder()
                    .apply { configureWebSocket() }
                    .buildAsync(URI(url), jdk11WebSocketListener)
                    .await()
            return Jdk11WebSocketConnection(webSocket, url, listener.incomingFrames)
        } catch (e: CancellationException) {
            throw e // this is an upstream exception that we don't want to wrap here
        } catch (e: WebSocketHandshakeException) {
            throw WebSocketConnectionException(url, httpStatusCode = e.response.statusCode(), cause = e)
        } catch (e: Exception) {
            throw WebSocketConnectionException(url, httpStatusCode = null, cause = e)
        }
    }
}

private class Jdk11WebSocketListener(
    private val listener: WebSocketListenerFlowAdapter
) : WebSocket.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("krossbow-jdk11-ws-listener-adapter"))

    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> = scope.future {
        listener.onTextMessage(data, last)
        // The call to request(1) here is to ensure that onText() is not called again before the (potentially partial)
        // message has been processed
        webSocket.request(1)
    }

    override fun onBinary(webSocket: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage<*> = scope.future {
        listener.onBinaryMessage(data.toByteArray(), last)
        // The call to request(1) here is to ensure that onBinary() is not called again before the (potentially partial)
        // message has been processed
        webSocket.request(1)
    }

    override fun onPing(webSocket: WebSocket, message: ByteBuffer): CompletionStage<*> = scope.future {
        listener.onPing(message.toByteArray())
        webSocket.request(1)
    }

    override fun onPong(webSocket: WebSocket, message: ByteBuffer): CompletionStage<*> = scope.future {
        listener.onPong(message.toByteArray())
        webSocket.request(1)
    }

    override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String?): CompletionStage<*> = scope.future {
        listener.onClose(statusCode, reason)
        scope.cancel()
    }

    override fun onError(webSocket: WebSocket, error: Throwable?) {
        listener.onError(error)
        scope.cancel()
    }
}

private fun ByteBuffer.toByteArray(): ByteArray {
    val array = ByteArray(remaining())
    get(array)
    return array
}

/**
 * An adapter wrapping JDK11's async [WebSocket] as a [WebSocketConnection].
 */
private class Jdk11WebSocketConnection(
    private val webSocket: WebSocket,
    override val url: String,
    override val incomingFrames: Flow<WebSocketFrame>,
) : WebSocketConnectionWithPingPong {

    private val mutex = Mutex()

    override val canSend: Boolean
        get() = !webSocket.isOutputClosed

    override suspend fun sendText(frameText: String) {
        mutex.withLock {
            webSocket.sendText(frameText, true).await()
        }
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        mutex.withLock {
            webSocket.sendBinary(ByteBuffer.wrap(frameData), true).await()
        }
    }

    override suspend fun sendPing(frameData: ByteArray) {
        mutex.withLock {
            webSocket.sendPing(ByteBuffer.wrap(frameData)).await()
        }
    }

    override suspend fun sendPong(frameData: ByteArray) {
        mutex.withLock {
            webSocket.sendPong(ByteBuffer.wrap(frameData)).await()
        }
    }

    override suspend fun close(code: Int, reason: String?) {
        mutex.withLock {
            webSocket.sendClose(code, reason ?: "").await()
        }
    }
}
