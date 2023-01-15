package org.hildan.krossbow.websocket.darwin

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.loader.WebSocketClientConfig
import org.hildan.krossbow.websocket.loader.WebSocketClientFactoriesRegistry
import org.hildan.krossbow.websocket.loader.WebSocketClientFactory

/**
 * This is just a trick to force Kotlin to load the factory class so it registers itself for discovery.
 */
@Suppress("DEPRECATION", "unused")
@OptIn(ExperimentalStdlibApi::class)
@EagerInitialization
private val initHook = DarwinWebSocketClientFactory

class DarwinWebSocketClientConfig : WebSocketClientConfig

object DarwinWebSocketClientFactory : WebSocketClientFactory<DarwinWebSocketClientConfig> {
    init {
        WebSocketClientFactoriesRegistry.register(this)
    }

    override fun create(configure: DarwinWebSocketClientConfig.() -> Unit): WebSocketClient = DarwinWebSocketClient()
}
