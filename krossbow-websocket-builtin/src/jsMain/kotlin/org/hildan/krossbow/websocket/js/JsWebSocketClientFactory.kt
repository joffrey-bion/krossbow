package org.hildan.krossbow.websocket.js

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
private val initHook = BrowserWebSocketClientFactory

class BrowserWebSocketClientConfig : WebSocketClientConfig

object BrowserWebSocketClientFactory : WebSocketClientFactory<BrowserWebSocketClientConfig> {
    init {
        WebSocketClientFactoriesRegistry.register(this)
    }

    override fun create(configure: BrowserWebSocketClientConfig.() -> Unit): WebSocketClient = BrowserWebSocketClient
}
