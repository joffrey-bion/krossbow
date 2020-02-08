package org.hildan.krossbow.engines.mpp.js

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.hildan.krossbow.websocket.KWebSocket
import org.hildan.krossbow.websocket.KWebSocketListener
import org.hildan.krossbow.websocket.KWebSocketSession
import org.hildan.krossbow.websocket.NoopWebsocketListener
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.ARRAYBUFFER
import org.w3c.dom.BinaryType
import org.w3c.dom.WebSocket
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object JsWebSocket: KWebSocket {

    override suspend fun connect(url: String): KWebSocketSession {
        return suspendCoroutine { cont ->
            val ws = WebSocket(url)
            ws.binaryType = BinaryType.ARRAYBUFFER // to receive arraybuffer instead of blob
            val wsSession = JsWebSocketSession(ws)
            ws.onopen = { cont.resume(wsSession) }
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

private fun ArrayBuffer.toByteArray(): ByteArray {
    val int8Array = Int8Array(this)
    return ByteArray(int8Array.length) { int8Array[it] }
}

private fun ByteArray.toArrayBuffer(): ArrayBuffer {
    return Int8Array(this.toTypedArray()).buffer
}

class JsWebSocketSession(val ws: WebSocket) : KWebSocketSession {

    override var listener: KWebSocketListener = NoopWebsocketListener

    override suspend fun send(frameData: ByteArray) {
        ws.send(frameData.toArrayBuffer())
    }

    override suspend fun close() {
        ws.close()
    }
}
