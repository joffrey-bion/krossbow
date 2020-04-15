package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.js.BrowserWebSocketClient

actual fun defaultWebSocketClient(): WebSocketClient = BrowserWebSocketClient
