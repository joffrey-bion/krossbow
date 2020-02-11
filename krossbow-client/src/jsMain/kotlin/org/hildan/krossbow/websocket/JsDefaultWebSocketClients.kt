package org.hildan.krossbow.websocket

import org.hildan.krossbow.engines.mpp.js.BrowserWebSocketClient
import org.hildan.krossbow.engines.mpp.js.SockJSWebSocketClient

actual fun defaultWebSocketClient(): KWebSocketClient = BrowserWebSocketClient

actual fun defaultSockJSClient(): KWebSocketClient = SockJSWebSocketClient
