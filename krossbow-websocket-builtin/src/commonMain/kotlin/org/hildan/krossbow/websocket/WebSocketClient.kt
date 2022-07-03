package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.builtin.builtIn

@Deprecated(
    message = "Renamed to builtIn(), and moved to the package org.hildan.krossbow.websocket.builtin.builtIn",
    ReplaceWith("builtIn()", imports = ["org.hildan.krossbow.websocket.builtin.builtIn"]),
)
expect fun WebSocketClient.Companion.default(): WebSocketClient
