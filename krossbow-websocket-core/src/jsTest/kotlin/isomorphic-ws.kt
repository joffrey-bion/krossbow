@JsModule("isomorphic-ws")
external class WebSocket(url: String)

/**
 * Creates a [WebSocket][org.w3c.dom.WebSocket]-like instance using `isomorphic-ws`.
 * It uses the browser's [WebSocket][org.w3c.dom.WebSocket] when available, or falls back to `ws` on NodeJS.
 */
@Suppress("TestFunctionName")
fun IsomorphicWebSocket(url: String) = WebSocket(url).unsafeCast<org.w3c.dom.WebSocket>()