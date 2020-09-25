package org.hildan.krossbow.websocket.jdk

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
            return Jdk11WebSocketSession(webSocket, listener.incomingFrames)
        } catch (e: CancellationException) {
            throw e // this is an upstream exception that we don't want to wrap here
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

    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? = asyncFuture {
        listener.onTextMessage(data, last)
        // The completion of the returned CompletionStage is only used to reclaim the CharSequence.
        // The onText() method itself can be called again as soon as it completes, which can cause concurrency issues.
        // This re-entrance is however controlled by the invocations counter, incremented by calling request(n).
        // The call to request(1) located after the message processing thus prevents issues.
        webSocket.request(1)
    }

    override fun onBinary(webSocket: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage<*>? = asyncFuture {
        listener.onBinaryMessage(data.toByteArray(), last)
        // The completion of the returned CompletionStage is only used to reclaim the CharSequence.
        // The onBinary() method itself can be called again as soon as it completes, which can cause concurrency issues.
        // This re-entrance is however controlled by the invocations counter, incremented by calling request(n).
        // The call to request(1) here is to ensure that onBinary() is not called again
        // before the (potentially partial) message has been processed
        webSocket.request(1)
    }

    override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String?): CompletionStage<*>? = asyncFuture {
        listener.onClose(statusCode, reason)
        job.cancel()
    }

    override fun onError(webSocket: WebSocket, error: Throwable?) {
        listener.onError(error)
        job.cancel()
    }
}

private fun CoroutineScope.asyncFuture(block: suspend CoroutineScope.() -> Unit) =
    async(block = block).asCompletableFuture()

private fun ByteBuffer.toByteArray(): ByteArray {
    val array = ByteArray(remaining())
    get(array)
    return array
}

/**
 * An adapter wrapping JDK11's async [WebSocket] as a [WebSocketSession].
 */
private class Jdk11WebSocketSession(
    private val webSocket: WebSocket,
    override val incomingFrames: ReceiveChannel<WebSocketFrame>
) : WebSocketSession {

    override val canSend: Boolean
        get() = !webSocket.isOutputClosed

    override suspend fun sendText(frameText: String) {
        webSocket.sendText(frameText, true).await()
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        webSocket.sendBinary(ByteBuffer.wrap(frameData), true).await()
    }

    override suspend fun close(code: Int, reason: String?) {
        webSocket.sendClose(code, reason ?: "").await()
    }
}
