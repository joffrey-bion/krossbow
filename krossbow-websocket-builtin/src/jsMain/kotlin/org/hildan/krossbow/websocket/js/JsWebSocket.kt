package org.hildan.krossbow.websocket.js

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.hildan.krossbow.websocket.UnboundedWsListenerFlowAdapter
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection
import org.hildan.krossbow.websocket.WebSocketConnectionClosedException
import org.hildan.krossbow.websocket.WebSocketConnectionException
import org.hildan.krossbow.websocket.WebSocketFrame
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.ARRAYBUFFER
import org.w3c.dom.BinaryType
import org.w3c.dom.CloseEvent
import org.w3c.dom.ErrorEvent
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Default WebSocket found in the browser. Not supported in NodeJS environment.
 */
object BrowserWebSocketClient : JsWebSocketClientAdapter({ url -> WebSocket(url) })

/**
 * A [WebSocketClient] adapting JavaScript [WebSocket] objects to [WebSocketConnection]s.
 */
open class JsWebSocketClientAdapter(
    /**
     * A function to create [WebSocket] connections from a given URL.
     */
    private val newWebSocket: (String) -> WebSocket
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketConnection {
        return suspendCancellableCoroutine { cont ->
            try {
                val ws = newWebSocket(url)
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
                        is ArrayBuffer -> listener.onBinaryMessage(body.toByteArray())
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
 * A adapter wrapping a JavaScript [WebSocket] object as a [WebSocketConnection].
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

    override suspend fun sendBinary(frameData: ByteArray) {
        ws.send(frameData.toArrayBuffer())
    }

    override suspend fun close(code: Int, reason: String?) {
        ws.close(code.toShort(), reason ?: "")
    }
}

private fun ArrayBuffer.toByteArray(): ByteArray {
    val int8Array = Int8Array(this)
    return ByteArray(int8Array.length) { int8Array[it] }
}

private fun ByteArray.toArrayBuffer(): ArrayBuffer = Int8Array(this.toTypedArray()).buffer
