import org.hildan.krossbow.websocket.js.JsWebSocketClient

// both annotations, so it's accessible from UMD
@JsModule("isomorphic-ws")
@JsNonModule
external class WebSocket(url: String)

/**
 * An [WebSocketClient][org.hildan.krossbow.websocket.WebSocketClient] based on a
 * [WebSocket][org.w3c.dom.WebSocket]-like instance using `isomorphic-ws`.
 * It uses the browser's [WebSocket][org.w3c.dom.WebSocket] when available, or falls back to `ws` on NodeJS.
 */
object IsomorphicJsWebSocketClient : JsWebSocketClient {
    override fun newWebSocket(url: String, headers: Map<String, String>): org.w3c.dom.WebSocket {
        require(headers.isEmpty()) {
            "custom HTTP headers are not supported by the isomorphic WS client because they are not supported in the " +
                    "browser, see https://github.com/whatwg/websockets/issues/16"
        }
        return WebSocket(url).unsafeCast<org.w3c.dom.WebSocket>()
    }
}
