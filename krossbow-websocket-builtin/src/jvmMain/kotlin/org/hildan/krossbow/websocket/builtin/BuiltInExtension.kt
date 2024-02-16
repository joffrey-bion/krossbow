package org.hildan.krossbow.websocket.builtin

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.jdk.Jdk11WebSocketClient

/**
 * Gets the [WebSocketClient] implementation that adapts the built-in client from the JVM platform.
 */
actual fun WebSocketClient.Companion.builtIn(): WebSocketClient = Jdk11WebSocketClient()
