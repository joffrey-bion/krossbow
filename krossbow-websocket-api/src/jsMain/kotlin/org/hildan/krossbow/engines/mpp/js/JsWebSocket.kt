package org.hildan.krossbow.engines.mpp.js

import SockJS
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.hildan.krossbow.websocket.KWebSocketClient
import org.hildan.krossbow.websocket.KWebSocketListener
import org.hildan.krossbow.websocket.KWebSocketSession
import org.hildan.krossbow.websocket.NoopWebSocketListener
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.ARRAYBUFFER
import org.w3c.dom.BinaryType
import org.w3c.dom.WebSocket
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Default WebSocket found in the browser. Not supported in NodeJS environment.
 */
object BrowserWebSocketClient: JsWebSocketClientAdapter({ url -> WebSocket(url) })

/**
 * SockJS WebSocket client, compatible with browsers and NodeJS, but requires a SockJS-compliant server.
 */
object SockJSWebSocketClient: JsWebSocketClientAdapter({ url -> SockJS(url) })

open class JsWebSocketClientAdapter(val newWebSocket: (String) -> WebSocket): KWebSocketClient {

    override suspend fun connect(url: String): KWebSocketSession {
        return suspendCoroutine { cont ->
            val ws = newWebSocket(url)
            ws.binaryType = BinaryType.ARRAYBUFFER // to receive arraybuffer instead of blob
            val wsSession = JsWebSocketSession(ws)
            ws.onopen = {
                cont.resume(wsSession)
            }
            ws.onclose = {
                GlobalScope.promise {
                    wsSession.listener.onClose()
                }
            }
            ws.onerror = { errEvent ->
                // TODO maybe handle connection failure via continuation here (check whether already successfully open)
                GlobalScope.promise {
                    wsSession.listener.onError(Exception("WebSocket error: $errEvent"))
                }
            }
            ws.onmessage = { event ->
                GlobalScope.promise {
                    // TODO check the possible types here
                    when (val body = event.data) {
                        is String -> wsSession.listener.onTextMessage(body)
                        is ArrayBuffer -> wsSession.listener.onBinaryMessage(body.toByteArray())
                        null -> wsSession.listener.onBinaryMessage(ByteArray(0))
                        else -> error("Unknown socket frame body type: ${body::class.js}")
                    }
                }
            }
        }
    }
}

class JsWebSocketSession(private val ws: WebSocket) : KWebSocketSession {

    override var listener: KWebSocketListener = NoopWebSocketListener

    override suspend fun sendText(frameText: String) {
        ws.send(frameText)
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        ws.send(frameData.toArrayBuffer())
    }

    override suspend fun close() {
        ws.close()
    }
}

private fun ArrayBuffer.toByteArray(): ByteArray {
    val int8Array = Int8Array(this)
    return ByteArray(int8Array.length) { int8Array[it] }
}

private fun ByteArray.toArrayBuffer(): ArrayBuffer = Int8Array(this.toTypedArray()).buffer
