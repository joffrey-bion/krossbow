package org.hildan.krossbow.websocket.builtin

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.jdk.Jdk11WebSocketClient

actual fun WebSocketClient.Companion.builtIn(): WebSocketClient = Jdk11WebSocketClient()
