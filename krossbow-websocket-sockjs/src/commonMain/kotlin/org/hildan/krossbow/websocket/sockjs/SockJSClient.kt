package org.hildan.krossbow.websocket.sockjs

import org.hildan.krossbow.websocket.WebSocketClient

/**
 * Multiplatform SockJS client, which falls back to other transports if WebSockets are missing.
 * This requires a SockJS-compliant server.
 */
@Suppress("FunctionName")
expect fun SockJSClient(): WebSocketClient
