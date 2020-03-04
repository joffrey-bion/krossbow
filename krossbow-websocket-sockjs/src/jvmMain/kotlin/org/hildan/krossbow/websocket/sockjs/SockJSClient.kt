package org.hildan.krossbow.websocket.sockjs

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.spring.SpringSockJSWebSocketClient

@Suppress("FunctionName")
actual fun SockJSClient(): WebSocketClient = SpringSockJSWebSocketClient
