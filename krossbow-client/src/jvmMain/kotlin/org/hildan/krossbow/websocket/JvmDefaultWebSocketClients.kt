package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.spring.SpringDefaultWebSocketClient
import org.hildan.krossbow.websocket.spring.SpringSockJSWebSocketClient

actual fun defaultWebSocketClient(): KWebSocketClient = SpringDefaultWebSocketClient

actual fun defaultSockJSClient(): KWebSocketClient = SpringSockJSWebSocketClient
