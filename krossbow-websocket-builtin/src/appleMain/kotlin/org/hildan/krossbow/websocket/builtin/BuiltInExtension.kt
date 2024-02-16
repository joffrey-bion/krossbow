package org.hildan.krossbow.websocket.builtin

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.darwin.DarwinWebSocketClient

/**
 * Gets the [WebSocketClient] implementation that adapts the built-in client from the Darwin platform.
 */
actual fun WebSocketClient.Companion.builtIn(): WebSocketClient = DarwinWebSocketClient()
