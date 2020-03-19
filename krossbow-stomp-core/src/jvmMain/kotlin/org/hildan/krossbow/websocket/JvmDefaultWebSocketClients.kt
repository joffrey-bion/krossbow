package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.jdk.Jdk11WebSocketClient

actual fun defaultWebSocketClient(): WebSocketClient = Jdk11WebSocketClient()
