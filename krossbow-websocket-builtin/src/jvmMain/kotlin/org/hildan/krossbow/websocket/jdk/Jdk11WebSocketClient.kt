package org.hildan.krossbow.websocket.jdk

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.*
import kotlinx.io.bytestring.*
import org.hildan.krossbow.io.*
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

    override val supportsCustomHeaders: Boolean = true

    override suspend fun connect(url: String, protocols: List<String>, headers: Map<String, String>): WebSocketConnectionWithPingPong {
        try {
            val listener = WebSocketListenerFlowAdapter()
            val jdk11WebSocketListener = Jdk11WebSocketListener(listener)
            val webSocket = httpClient.newWebSocketBuilder()
                    .apply {
                        headers.forEach { (key, value) ->
                            header(key, value)
                        }
                        if (protocols.isNotEmpty()) {
                            subprotocols(protocols[0], *protocols.drop(1).toTypedArray())
                        }
                        configureWebSocket()
                    }
                    .buildAsync(URI(url), jdk11WebSocketListener)
                    .await()
            return Jdk11WebSocketConnection(webSocket, url, listener.incomingFrames)
        } catch (e: CancellationException) {
            throw e // this is an upstream exception that we don't want to wrap here
        } catch (e: WebSocketHandshakeException) {
            throw WebSocketConnectionException(
                url = url,
                httpStatusCode = e.response.statusCode(),
                additionalInfo = (e.response.body() as? String)?.takeIf { it.isNotBlank() },
                cause = e
            )
        } catch (e: Exception) {
            throw WebSocketConnectionException(url, httpStatusCode = null, additionalInfo = null, cause = e)
        }
    }
}

private class Jdk11WebSocketListener(
    private val listener: WebSocketListenerFlowAdapter
) : WebSocket.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("krossbow-jdk11-ws-listener-adapter"))

    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> = scope.future {
        listener.onTextMessage(last) {
            // currently there is nothing better to write UTF-8
            // https://github.com/Kotlin/kotlinx-io/issues/261
            writeString(data.toString())
        }
        // The call to request(1) here is to ensure that onText() is not called again before the (potentially partial)
        // message has been processed
        webSocket.request(1)
    }

    override fun onBinary(webSocket: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage<*> = scope.future {
        listener.onBinaryMessage(last) { write(data) }
        // The call to request(1) here is to ensure that onBinary() is not called again before the (potentially partial)
        // message has been processed
        webSocket.request(1)
    }

    override fun onPing(webSocket: WebSocket, message: ByteBuffer): CompletionStage<*> = scope.future {
        listener.onPing(message.readByteString())
        webSocket.request(1)
    }

    override fun onPong(webSocket: WebSocket, message: ByteBuffer): CompletionStage<*> = scope.future {
        listener.onPong(message.readByteString())
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

/**
 * An adapter wrapping JDK11's async [WebSocket] as a [WebSocketConnection].
 */
private class Jdk11WebSocketConnection(
    private val webSocket: WebSocket,
    override val url: String,
    override val incomingFrames: Flow<WebSocketFrame>,
) : WebSocketConnectionWithPingPong {

    // send operations must not be called concurrently as per the JDK11 documentation
    private val mutex = Mutex()

    override val protocol: String? = webSocket.subprotocol?.ifEmpty { null }

    override val canSend: Boolean
        get() = !webSocket.isOutputClosed

    override suspend fun sendText(frameText: String) {
        mutex.withLock {
            webSocket.sendText(frameText, true).await()
        }
    }

    override suspend fun sendBinary(frameData: ByteString) {
        mutex.withLock {
            webSocket.sendBinary(frameData.asReadOnlyByteBuffer(), true).await()
        }
    }

    override suspend fun sendPing(frameData: ByteString) {
        mutex.withLock {
            webSocket.sendPing(frameData.asReadOnlyByteBuffer()).await()
        }
    }

    override suspend fun sendPong(frameData: ByteString) {
        mutex.withLock {
            webSocket.sendPong(frameData.asReadOnlyByteBuffer()).await()
        }
    }

    override suspend fun close(code: Int, reason: String?) {
        mutex.withLock {
            webSocket.sendClose(code, reason ?: "").await()
        }
    }
}
