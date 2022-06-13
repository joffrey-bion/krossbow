package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.js.BrowserWebSocketClient

actual fun WebSocketClient.Companion.default(): WebSocketClient = BrowserWebSocketClient
