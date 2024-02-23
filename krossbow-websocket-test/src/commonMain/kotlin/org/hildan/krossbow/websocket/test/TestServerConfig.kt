package org.hildan.krossbow.websocket.test

expect fun getTestServerConfig(): TestServerConfig

data class TestServerConfig(
    val host: String,
    val wsPort: Int,
    val httpPort: Int,
) {
    val wsUrl: String = "ws://$host:$wsPort"

    // we need ws:// scheme because the browser web socket doesn't support anything else
    val wsUrlWithHttpPort: String = "ws://$host:$httpPort"
}
