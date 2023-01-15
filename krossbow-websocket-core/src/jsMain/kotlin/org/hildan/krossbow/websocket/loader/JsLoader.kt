package org.hildan.krossbow.websocket.loader

import org.hildan.krossbow.websocket.WebSocketClient

/**
 * Gets the first [WebSocketClient] implementation in the dependencies for the current platform.
 */
actual fun WebSocketClient.Companion.fromDependencies(): WebSocketClientFactory<*> =
    WebSocketClientFactoriesRegistry.find()
        ?: error("No auto-registered JS web socket implementation found. " +
                "Consider adding a dependency on a Krossbow web socket implementation in JS source sets.")
