package org.hildan.krossbow.websocket.sockjs

import SockJS
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.js.JsWebSocketClientAdapter

@Suppress("FunctionName")
actual fun SockJSClient(): WebSocketClient = JsWebSocketClientAdapter { url -> SockJS(url) }
