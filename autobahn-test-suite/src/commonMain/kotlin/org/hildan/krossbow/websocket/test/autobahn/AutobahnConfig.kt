package org.hildan.krossbow.websocket.test.autobahn

expect fun getDefaultAutobahnConfig(): AutobahnConfig

data class AutobahnConfig(
    val host: String,
    val wsPort: Int,
    val webPort: Int,
) {
    val websocketTestServerUrl: String = "ws://$host:$wsPort"
}
