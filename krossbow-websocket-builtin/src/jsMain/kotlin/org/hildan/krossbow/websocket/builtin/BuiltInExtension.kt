package org.hildan.krossbow.websocket.builtin

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.js.BrowserWebSocketClient

actual fun WebSocketClient.Companion.builtIn(): WebSocketClient = BrowserWebSocketClient
