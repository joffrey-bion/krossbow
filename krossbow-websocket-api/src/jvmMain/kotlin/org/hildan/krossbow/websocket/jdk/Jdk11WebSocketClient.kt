package org.hildan.krossbow.websocket.jdk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnectionException
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketListenerChannelAdapter
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

    private val textAccumulator = StringBuilder()

    private var binaryBuffer = ByteBuffer.allocate(16)

    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
        webSocket.request(1)
        textAccumulator.append(data)
        if (!last) {
            return null
        }
        val text = textAccumulator.toString()
        textAccumulator.clear()
        return async { listener.onTextMessage(text) }.asCompletableFuture()
    }

    override fun onBinary(webSocket: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage<*>? {
        webSocket.request(1)
        ensureCapacityFor(data.remaining())
        binaryBuffer.put(data)
        if (!last) {
            return null
        }
        val array = binaryBuffer.toByteArray()
        binaryBuffer.clear()
        return async { listener.onBinaryMessage(array) }.asCompletableFuture()
    }

    private fun ensureCapacityFor(nBytes: Int) {
        if (binaryBuffer.remaining() < nBytes) {
            binaryBuffer = binaryBuffer.grownBy(nBytes)
        }
    }

    override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String?): CompletionStage<*>? {
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

private fun ByteBuffer.grownBy(nBytes: Int): ByteBuffer? {
    val newBuffer = ByteBuffer.allocate(position() + nBytes)
    newBuffer.put(this)
    return newBuffer
}

private fun ByteBuffer.toByteArray(): ByteArray {
    val array = ByteArray(position())
    position(0)
    get(array)
    return array
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
