package org.hildan.krossbow.websocket

actual fun defaultWebSocketClient(): WebSocketClient =
    throw NotImplementedError("There is no default web socket client on iOS target at the moment, please provide your" +
        " own client implementation.")
