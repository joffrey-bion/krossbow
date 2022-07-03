package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.builtin.builtIn

actual fun WebSocketClient.Companion.default(): WebSocketClient = builtIn()
