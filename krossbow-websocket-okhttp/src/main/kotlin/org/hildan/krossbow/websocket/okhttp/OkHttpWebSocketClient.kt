package org.hildan.krossbow.websocket.okhttp

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.io.bytestring.unsafe.*
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.hildan.krossbow.io.*
import org.hildan.krossbow.websocket.*
import java.net.SocketException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.hildan.krossbow.websocket.WebSocketClient as KrossbowWebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection as KrossbowWebSocketSession

/**
 * This header specifies one or more (comma-separated) WebSocket protocols that you wish to use, in order of
 * preference. The first one that is supported by the server will be selected and returned by the server in a
 * `Sec-WebSocket-Protocol` header included in the response.
 */
private const val SecWebSocketProtocol = "Sec-WebSocket-Protocol"

class OkHttpWebSocketClient(
    private val client: OkHttpClient = OkHttpClient(),
) : KrossbowWebSocketClient {

    override val supportsCustomHeaders: Boolean = true

    override suspend fun connect(url: String, protocols: List<String>, headers: Map<String, String>): KrossbowWebSocketSession {
        val request = Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .apply {
                if (protocols.isNotEmpty()) {
                    header(SecWebSocketProtocol, protocols.joinToString(", "))
                }
            }
            .build()
        val channelListener = WebSocketListenerFlowAdapter()

        return suspendCancellableCoroutine { continuation ->
            val okHttpListener = KrossbowToOkHttpListenerAdapter(continuation, url, channelListener)
            val ws = client.newWebSocket(request, okHttpListener)
            continuation.invokeOnCancellation {
                ws.cancel()
            }
        }
    }
}

// Note: OkHttp's listener is only ever called on the same thread (web socket reader thread), so we apparently don't 
// need synchronization to protect our WebSocketListenerFlowAdapter here.
private class KrossbowToOkHttpListenerAdapter(
    connectionContinuation: Continuation<KrossbowWebSocketSession>,
    private val originalConnectionUrl: String,
    private val channelListener: WebSocketListenerFlowAdapter,
) : WebSocketListener() {
    private var connectionContinuation: Continuation<KrossbowWebSocketSession>? = connectionContinuation

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
                url = originalConnectionUrl, // webSocket.request().url returns an HTTP URL even when using 'ws://'
                httpStatusCode =  response?.code ?: invalidStatusCodeFromErrorMessage(t.message),
                additionalInfo = responseBody ?: t.toString(),
                cause = t,
            )
            completeConnection {
                resumeWithException(exception)
            }
        } else {
            channelListener.onError(t)
        }
    }

    private fun invalidStatusCodeFromErrorMessage(message: String?): Int? =
        // for some reason, in that case, the response is null and we can't get the status code from there
        if (message == "Received HTTP_PROXY_AUTH (407) code while not using proxy") 407 else null
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
