package org.hildan.krossbow.websocket.js

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.io.bytestring.*
import org.hildan.krossbow.io.*
import org.hildan.krossbow.websocket.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import kotlin.coroutines.*

/**
 * Default WebSocket found in the browser. Not supported in Node.js environment.
 */
object BrowserWebSocketClient : JsWebSocketClient {

    override val supportsCustomHeaders: Boolean = false

    override fun newWebSocket(url: String, headers: Map<String, String>): WebSocket {
        require(headers.isEmpty()) {
            "custom HTTP headers are not supported in the browser, see https://github.com/whatwg/websockets/issues/16"
        }
        return WebSocket(url)
    }
}

/**
 * A [WebSocketClient] adapting JavaScript [WebSocket] objects to [WebSocketConnection]s.
 *
 * This interface is not stable for inheritance by third parties, compatibility is not guaranteed.
 */
interface JsWebSocketClient : WebSocketClient {

    /**
     * Creates a [WebSocket] connection to the given [url], using the provided [headers] in the handshake.
     *
     * This function may throw an exception if [headers] is not empty and the underlying implementation doesn't support
     * custom headers in the handshake.
     */
    fun newWebSocket(url: String, headers: Map<String, String>): WebSocket

    override suspend fun connect(url: String, headers: Map<String, String>): WebSocketConnection {
        return suspendCancellableCoroutine { cont ->
            try {
                val ws = newWebSocket(url, headers)
                ws.binaryType = BinaryType.ARRAYBUFFER // to receive arraybuffer instead of blob
                var pendingConnect = true
                // We use unlimited buffer size because we have no means for backpressure anyway
                // (see motivation for https://www.chromestatus.com/feature/5189728691290112)
                val listener = UnboundedWsListenerFlowAdapter()
                val wsSession = JsWebSocketConnection(ws, listener.incomingFrames)
                ws.onopen = {
                    pendingConnect = false
                    cont.resume(wsSession)
                }
                ws.onclose = { event: Event ->
                    val closeEvent: CloseEvent = event.unsafeCast<CloseEvent>()
                    val code = closeEvent.code.toInt()
                    if (pendingConnect) {
                        pendingConnect = false
                        cont.resumeWithException(WebSocketConnectionClosedException(
                            url = url,
                            code = code,
                            reason = closeEvent.reason,
                        ))
                    } else {
                        listener.onClose(code, closeEvent.reason)
                    }
                }
                ws.onerror = { event ->
                    val errorEvent: ErrorEvent = event.unsafeCast<ErrorEvent>()
                    if (pendingConnect) {
                        pendingConnect = false
                        cont.resumeWithException(WebSocketConnectionException(
                            url = url,
                            // It is by design (for security reasons) that browsers don't give access to the status code.
                            // See the end of the section about feedback here:
                            // https://websockets.spec.whatwg.org//#feedback-from-the-protocol
                            // TODO check whether this can be provided in nodejs environments
                            httpStatusCode = null,
                            additionalInfo = "error details hidden for security reasons",
                            message = errorEvent.message,
                        ))
                    } else {
                        listener.onError(errorEvent.message)
                    }
                }
                ws.onmessage = { event ->
                    // Types defined by the specification here:
                    // https://html.spec.whatwg.org/multipage/web-sockets.html#feedback-from-the-protocol
                    // Because ws.binaryType was set to ARRAYBUFFER, we should never receive Blob objects
                    when (val body = event.data) {
                        is ArrayBuffer -> listener.onBinaryMessage(body.toByteString())
                        is String -> listener.onTextMessage(body)
                        null -> listener.onTextMessage("")
                        else -> listener.onError("Unknown socket frame body type: ${body::class.js}")
                    }
                }
            } catch (e: Exception) {
                console.error("Exception in WebSocket setup: ${e.message}")
                cont.resumeWithException(e)
            }
        }
    }
}

/**
 * An adapter wrapping a JavaScript [WebSocket] object as a [WebSocketConnection].
 */
private class JsWebSocketConnection(
    private val ws: WebSocket,
    override val incomingFrames: Flow<WebSocketFrame>,
) : WebSocketConnection {

    override val url: String
        get() = ws.url

    override val canSend: Boolean
        get() = ws.readyState == WebSocket.OPEN

    override suspend fun sendText(frameText: String) {
        ws.send(frameText)
    }

    override suspend fun sendBinary(frameData: ByteString) {
        ws.send(frameData.toArrayBuffer())
    }

    override suspend fun close(code: Int, reason: String?) {
        ws.close(code.toShort(), reason ?: "")
    }
}
