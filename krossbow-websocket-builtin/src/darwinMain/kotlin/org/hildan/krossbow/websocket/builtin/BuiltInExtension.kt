package org.hildan.krossbow.websocket.builtin

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.darwin.DarwinWebSocketClient

actual fun WebSocketClient.Companion.builtIn(): WebSocketClient = DarwinWebSocketClient()
