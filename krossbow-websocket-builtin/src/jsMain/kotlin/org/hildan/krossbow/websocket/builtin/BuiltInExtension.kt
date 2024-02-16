package org.hildan.krossbow.websocket.builtin

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.js.BrowserWebSocketClient

/**
 * Gets the [WebSocketClient] implementation that adapts the built-in client from the JS platform.
 */
actual fun WebSocketClient.Companion.builtIn(): WebSocketClient = BrowserWebSocketClient
