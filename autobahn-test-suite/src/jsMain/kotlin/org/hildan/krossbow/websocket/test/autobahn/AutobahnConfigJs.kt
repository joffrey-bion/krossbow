package org.hildan.krossbow.websocket.test.autobahn

import org.hildan.krossbow.websocket.test.*

actual fun getDefaultAutobahnConfig() = readJsonAutobahnConfig().toCommonConfig()

private fun AutobahnConfigJson.toCommonConfig() = AutobahnConfig(
    host = host,
    wsPort = wsPort,
    webPort = webPort,
)

private external interface AutobahnConfigJson {
    val host: String
    val wsPort: Int
    val webPort: Int
}

private fun readJsonAutobahnConfig(): AutobahnConfigJson = when (currentJsPlatform()) {
    Platform.Js.Browser -> readJsonAutobahnConfigBrowser()
    Platform.Js.NodeJs -> readJsonAutobahnConfigNodeJS()
}

private fun readJsonAutobahnConfigBrowser(): AutobahnConfigJson = js("autobahn").unsafeCast<AutobahnConfigJson>()

private fun readJsonAutobahnConfigNodeJS(): AutobahnConfigJson =
    js("JSON.parse(require('fs').readFileSync('./autobahn-server.json', 'utf8'))").unsafeCast<AutobahnConfigJson>()
