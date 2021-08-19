package org.hildan.krossbow.websocket

@Deprecated(
    message = "This method pollutes the global namespace and will be removed in a future version. " +
        "Please use WebSocketClient.default() factory function instead",
    replaceWith = ReplaceWith("org.hildan.krossbow.websocket.WebSocketClient.default()"),
)
actual fun defaultWebSocketClient(): WebSocketClient = WebSocketClient.default()

actual fun WebSocketClient.Companion.default(): WebSocketClient =
    throw NotImplementedError("There is no default web socket client on iOS target at the moment, please provide your" +
        " own client implementation.")