package org.hildan.krossbow.websocket.js

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.hildan.krossbow.websocket.WebSocketListenerChannelAdapter
import org.hildan.krossbow.websocket.WebSocketConnectionClosedException
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketSession
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
 * A [WebSocketClient] adapting JavaScript [WebSocket] objects to [WebSocketSession]s.
 */
open class JsWebSocketClientAdapter(
    /**
     * A function to create [WebSocket] connections from a given URL.
     */
    private val newWebSocket: (String) -> WebSocket
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketSession {
        return suspendCancellableCoroutine { cont ->
            try {
                val ws = newWebSocket(url)
                ws.binaryType = BinaryType.ARRAYBUFFER // to receive arraybuffer instead of blob
                var pendingConnect = true
                val listener = WebSocketListenerChannelAdapter()
                val wsSession = JsWebSocketSession(ws, listener.incomingFrames)
                ws.onopen = {
                    pendingConnect = false
                    cont.resume(wsSession)
                }
                ws.onclose = { event: Event ->
                    val closeEvent: CloseEvent = event.unsafeCast<CloseEvent>()
                    val code = closeEvent.code.toInt()
                    if (pendingConnect) {
                        pendingConnect = false
                        cont.resumeWithException(WebSocketConnectionClosedException(url, code, closeEvent.reason))
                    } else {
                        GlobalScope.launch {
                            listener.onClose(code, closeEvent.reason)
                        }
                    }
                }
                ws.onerror = { event ->
                    val errorEvent: ErrorEvent = event.unsafeCast<ErrorEvent>()
                    if (pendingConnect) {
                        pendingConnect = false
                        cont.resumeWithException(WebSocketConnectionException(url, errorEvent.message))
                    } else {
                        GlobalScope.launch {
                            listener.onError(errorEvent.message)
                        }
                    }
                }
                ws.onmessage = { event ->
                    GlobalScope.launch {
                        // TODO check the possible types here
                        when (val body = event.data) {
                            is String -> listener.onTextMessage(body)
                            is ArrayBuffer -> listener.onBinaryMessage(body.toByteArray())
                            null -> listener.onTextMessage("")
                            else -> listener.onError("Unknown socket frame body type: ${body::class.js}")
                        }
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
 * A adapter wrapping a JavaScript [WebSocket] object as a [WebSocketSession].
 */
class JsWebSocketSession(
    private val ws: WebSocket,
    override val incomingFrames: ReceiveChannel<WebSocketFrame>
) : WebSocketSession {

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
