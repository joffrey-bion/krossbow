package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.spring.SpringDefaultWebSocketClient
import org.hildan.krossbow.websocket.spring.SpringSockJSWebSocketClient

actual fun defaultWebSocketClient(): WebSocketClient = SpringDefaultWebSocketClient

actual fun defaultSockJSClient(): WebSocketClient = SpringSockJSWebSocketClient
