package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.spring.SpringDefaultWebSocketClient

actual fun defaultWebSocketClient(): WebSocketClient = SpringDefaultWebSocketClient
