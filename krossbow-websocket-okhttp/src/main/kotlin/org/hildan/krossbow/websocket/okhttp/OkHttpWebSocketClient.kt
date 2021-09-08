package org.hildan.krossbow.websocket.okhttp

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.hildan.krossbow.websocket.WebSocketConnectionException
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketListenerChannelAdapter
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.hildan.krossbow.websocket.WebSocketClient as KrossbowWebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection as KrossbowWebSocketSession

class OkHttpWebSocketClient(
    private val client: OkHttpClient = OkHttpClient()
) : KrossbowWebSocketClient {

    override suspend fun connect(url: String): KrossbowWebSocketSession {
        val request = Request.Builder().url(url).build()
        val channelListener = WebSocketListenerChannelAdapter()

        return suspendCancellableCoroutine { continuation ->
            val okHttpListener = KrossbowToOkHttpListenerAdapter(continuation, channelListener)
            val ws = client.newWebSocket(request, okHttpListener)
            continuation.invokeOnCancellation {
                ws.cancel()
            }
        }
    }
}

private class KrossbowToOkHttpListenerAdapter(
    connectionContinuation: Continuation<KrossbowWebSocketSession>,
    private val channelListener: WebSocketListenerChannelAdapter,
) : WebSocketListener() {
    private var connectionContinuation: Continuation<KrossbowWebSocketSession>? = connectionContinuation

    @Volatile
    private var isConnecting = false

    private inline fun completeConnection(resume: Continuation<KrossbowWebSocketSession>.() -> Unit) {
        val cont = connectionContinuation ?: error("OkHttp connection continuation already consumed")
        connectionContinuation = null // avoid leaking the continuation
        isConnecting = false
        cont.resume()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        val krossbowConnection = OkHttpSocketToKrossbowConnectionAdapter(webSocket, channelListener.incomingFrames)
        completeConnection { resume(krossbowConnection) }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        runBlocking { channelListener.onBinaryMessage(bytes.toByteArray()) }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        runBlocking { channelListener.onTextMessage(text) }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        runBlocking { channelListener.onClose(code, reason) }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        if (isConnecting) {
            completeConnection {
                resumeWithException(WebSocketConnectionException(webSocket.request().url.toString(), cause = t))
            }
        } else {
            channelListener.onError(t)
        }
    }
}

private class OkHttpSocketToKrossbowConnectionAdapter(
    private val okSocket: WebSocket,
    framesChannel: ReceiveChannel<WebSocketFrame>
) : KrossbowWebSocketSession {

    override val url: String
        get() = okSocket.request().url.toString()

    override val canSend: Boolean
        get() = true // all send methods are just no-ops when the session is closed, so always OK

    override val incomingFrames: ReceiveChannel<WebSocketFrame> = framesChannel

    override suspend fun sendText(frameText: String) {
        okSocket.send(frameText)
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        okSocket.send(frameData.toByteString())
    }

    override suspend fun close(code: Int, reason: String?) {
        okSocket.close(code, reason)
    }
}
