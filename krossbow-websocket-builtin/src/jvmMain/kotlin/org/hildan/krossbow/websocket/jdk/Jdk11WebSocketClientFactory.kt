package org.hildan.krossbow.websocket.jdk

import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.loader.WebSocketClientConfig
import org.hildan.krossbow.websocket.loader.WebSocketClientFactory

class Jdk11WebSocketClientConfig : WebSocketClientConfig

class Jdk11WebSocketClientFactory : WebSocketClientFactory<Jdk11WebSocketClientConfig> {
    override fun create(configure: Jdk11WebSocketClientConfig.() -> Unit): WebSocketClient {
        return Jdk11WebSocketClient()
    }
}
