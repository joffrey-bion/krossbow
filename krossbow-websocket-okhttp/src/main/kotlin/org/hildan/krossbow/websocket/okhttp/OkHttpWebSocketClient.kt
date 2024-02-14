package org.hildan.krossbow.websocket.okhttp

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.io.bytestring.unsafe.*
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.hildan.krossbow.io.*
import org.hildan.krossbow.websocket.WebSocketConnectionException
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketListenerFlowAdapter
import java.net.SocketException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.hildan.krossbow.websocket.WebSocketClient as KrossbowWebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection as KrossbowWebSocketSession

class OkHttpWebSocketClient(
    private val client: OkHttpClient = OkHttpClient(),
) : KrossbowWebSocketClient {

    override suspend fun connect(url: String, headers: Map<String, String>): KrossbowWebSocketSession {
        val request = Request.Builder().url(url).headers(headers.toHeaders()).build()
        val channelListener = WebSocketListenerFlowAdapter()

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
    private val channelListener: WebSocketListenerFlowAdapter,
) : WebSocketListener() {
    private var connectionContinuation: Continuation<KrossbowWebSocketSession>? = connectionContinuation

    @Volatile
    private var isConnecting = true

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
        runBlocking { channelListener.onBinaryMessage(bytes.asByteBuffer().readByteString()) }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        runBlocking { channelListener.onTextMessage(text) }
    }

    // overriding onClosing and not onClosed because we want to receive the Close frame from the server directly
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        runBlocking { channelListener.onClose(code, reason) }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        if (isConnecting) {
            val responseBody = try {
                response?.body?.string()?.takeIf { it.isNotBlank() }
            } catch (e: SocketException) {
                // we can't always read the body when the connection failed
                t.addSuppressed(e)
                null
            }
            val exception = WebSocketConnectionException(
                url = webSocket.request().url.toString(),
                httpStatusCode =  response?.code,
                additionalInfo = responseBody,
                cause = t,
            )
            completeConnection {
                resumeWithException(exception)
            }
        } else {
            channelListener.onError(t)
        }
    }
}

private class OkHttpSocketToKrossbowConnectionAdapter(
    private val okSocket: WebSocket,
    override val incomingFrames: Flow<WebSocketFrame>,
) : KrossbowWebSocketSession {

    override val url: String
        get() = okSocket.request().url.toString()

    override val canSend: Boolean
        get() = true // all send methods are just no-ops when the session is closed, so always OK

    override suspend fun sendText(frameText: String) {
        okSocket.send(frameText)
    }

    @OptIn(UnsafeByteStringApi::class)
    override suspend fun sendBinary(frameData: kotlinx.io.bytestring.ByteString) {
        okSocket.send(frameData.unsafeBackingByteArray().toByteString())
    }

    override suspend fun close(code: Int, reason: String?) {
        okSocket.close(code, reason)
    }
}
