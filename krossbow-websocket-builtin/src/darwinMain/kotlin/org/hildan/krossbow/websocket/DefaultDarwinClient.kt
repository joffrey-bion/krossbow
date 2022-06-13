package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.darwin.DarwinWebSocketClient

actual fun WebSocketClient.Companion.default(): WebSocketClient = DarwinWebSocketClient()
