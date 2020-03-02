package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.js.BrowserWebSocketClient
import org.hildan.krossbow.websocket.js.SockJSWebSocketClient

actual fun defaultWebSocketClient(): WebSocketClient = BrowserWebSocketClient

actual fun defaultSockJSClient(): WebSocketClient = SockJSWebSocketClient
