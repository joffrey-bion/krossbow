import org.hildan.krossbow.websocket.js.JsWebSocketClientAdapter

// both annotations, so it's accessible from UMD
@JsModule("isomorphic-ws")
@JsNonModule
external class WebSocket(url: String)

/**
 * Creates a [WebSocket][org.w3c.dom.WebSocket]-like instance using `isomorphic-ws`.
 * It uses the browser's [WebSocket][org.w3c.dom.WebSocket] when available, or falls back to `ws` on NodeJS.
 */
@Suppress("TestFunctionName")
fun IsomorphicWebSocket(url: String) = WebSocket(url).unsafeCast<org.w3c.dom.WebSocket>()

/**
 * An [WebSocketClient][org.hildan.krossbow.websocket.WebSocketClient] based on a
 * [WebSocket][org.w3c.dom.WebSocket]-like instance using `isomorphic-ws`.
 * It uses the browser's [WebSocket][org.w3c.dom.WebSocket] when available, or falls back to `ws` on NodeJS.
 */
object IsomorphicJsWebSocketClient : JsWebSocketClientAdapter({ url -> IsomorphicWebSocket(url) })
