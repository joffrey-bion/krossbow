package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.js.BrowserWebSocketClient
import org.hildan.krossbow.websocket.js.SockJSWebSocketClient

actual fun defaultWebSocketClient(): KWebSocketClient = BrowserWebSocketClient

actual fun defaultSockJSClient(): KWebSocketClient = SockJSWebSocketClient
