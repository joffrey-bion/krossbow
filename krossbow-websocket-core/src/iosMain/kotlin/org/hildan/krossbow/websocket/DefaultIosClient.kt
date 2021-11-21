package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.ios.IosWebSocketClient

actual fun WebSocketClient.Companion.default(): WebSocketClient = IosWebSocketClient()
