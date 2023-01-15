package org.hildan.krossbow.websocket.loader

import org.hildan.krossbow.websocket.WebSocketClient
import java.util.ServiceLoader

/**
 * Gets the first [WebSocketClient] implementation in the dependencies for the current platform.
 */
actual fun WebSocketClient.Companion.fromDependencies(): WebSocketClientFactory<*> {
    val loader = ServiceLoader.load(WebSocketClientFactory::class.java)
    return loader.findFirst().orElse(null)
        ?: error("No JVM web socket implementation found via service loader. " +
                "Consider adding a dependency on a Krossbow web socket implementation in JVM source sets.")
}
