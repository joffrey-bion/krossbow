package org.hildan.krossbow.engines.mpp.js

import org.hildan.krossbow.websocket.KWebSocket
import org.hildan.krossbow.websocket.KWebSocketListener
import org.hildan.krossbow.websocket.KWebSocketSession
import org.w3c.dom.WebSocket
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object JsWebSocket: KWebSocket {
    override suspend fun connect(url: String): KWebSocketSession {
        return suspendCoroutine { cont ->
            val ws = WebSocket(url)
            ws.onopen = { cont.resume(JsWebSocketSession(ws)) }
        }
    }
}

class JsWebSocketSession(val ws: WebSocket) : KWebSocketSession {

    override var listener: KWebSocketListener
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    override suspend fun send(frameData: ByteArray) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
