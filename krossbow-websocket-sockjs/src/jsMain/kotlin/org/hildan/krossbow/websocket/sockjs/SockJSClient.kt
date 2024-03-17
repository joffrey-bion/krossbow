package org.hildan.krossbow.websocket.sockjs

import SockJS
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.js.JsWebSocketClient
import org.w3c.dom.WebSocket

@Suppress("FunctionName")
actual fun SockJSClient(): WebSocketClient = JavaScriptSockJSClient

object JavaScriptSockJSClient : JsWebSocketClient {

    override val supportsCustomHeaders: Boolean = false

    override fun newWebSocket(url: String, headers: Map<String, String>): WebSocket {
        require(headers.isEmpty()) {
            "custom HTTP headers are not supported by SockJS, see https://github.com/sockjs/sockjs-client/issues/196"
        }
        return SockJS(url)
    }
}
