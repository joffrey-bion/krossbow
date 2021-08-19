package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.jdk.Jdk11WebSocketClient

@Deprecated(
    message = "This method pollutes the global namespace and will be removed in a future version. " +
        "Please use WebSocketClient.default() factory function instead",
    replaceWith = ReplaceWith("org.hildan.krossbow.websocket.WebSocketClient.default()"),
)
actual fun defaultWebSocketClient(): WebSocketClient = WebSocketClient.default()

actual fun WebSocketClient.Companion.default(): WebSocketClient = Jdk11WebSocketClient()