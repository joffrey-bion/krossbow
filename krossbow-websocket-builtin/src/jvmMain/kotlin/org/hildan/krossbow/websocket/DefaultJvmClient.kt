package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.jdk.Jdk11WebSocketClient

actual fun WebSocketClient.Companion.default(): WebSocketClient = Jdk11WebSocketClient()
