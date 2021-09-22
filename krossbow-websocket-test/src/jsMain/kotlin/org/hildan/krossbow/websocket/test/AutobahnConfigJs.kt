package org.hildan.krossbow.websocket.test

actual fun getDefaultAutobahnTestServerHost(): String = when (isBrowser()) {
    true -> readAutobahnConfigBrowser().host
    false -> readAutobahnConfigNodeJS().host
}

actual fun getDefaultAutobahnTestServerPort(): Int = when (isBrowser()) {
    true -> readAutobahnConfigBrowser().port
    false -> readAutobahnConfigNodeJS().port
}

external interface AutobahnConfig {
    val host: String
    val port: Int
}

private fun readAutobahnConfigBrowser(): AutobahnConfig = js("autobahn").unsafeCast<AutobahnConfig>()

private fun readAutobahnConfigNodeJS(): AutobahnConfig =
    js("JSON.parse(require('fs').readFileSync('./autobahn-server.json', 'utf8'))").unsafeCast<AutobahnConfig>()
