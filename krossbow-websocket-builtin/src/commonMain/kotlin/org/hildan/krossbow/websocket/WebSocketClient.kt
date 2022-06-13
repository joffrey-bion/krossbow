package org.hildan.krossbow.websocket

/**
 * Gets the default [WebSocketClient] implementation for the current platform.
 */
expect fun WebSocketClient.Companion.default(): WebSocketClient
