package org.hildan.krossbow.websocket.builtin

import org.hildan.krossbow.websocket.WebSocketClient

/**
 * Gets the [WebSocketClient] implementation that adapts the built-in client from the current platform.
 */
expect fun WebSocketClient.Companion.builtIn(): WebSocketClient
