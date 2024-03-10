package org.hildan.krossbow.websocket.test

actual fun getTestServerConfig(): TestServerConfig = when (currentWasmJsPlatform()) {
    Platform.WasmJs.Browser -> getTestServerConfigBrowser().toCommonConfig()
    Platform.WasmJs.NodeJs -> getTestServerConfigFromEnv()
}

// This variable is defined using the webpack DefinePlugin in karma.config.d/<somename>.js
// which is itself generated from Gradle in the test-server plugin
private fun getTestServerConfigBrowser(): TestServerConfigJson = js("testServerConfig")

private external interface TestServerConfigJson {
    val host: String
    val wsPort: Int
    val httpPort: Int
}

private fun TestServerConfigJson.toCommonConfig() = TestServerConfig(
    host = host,
    wsPort = wsPort,
    httpPort = httpPort,
)

private fun getTestServerConfigFromEnv(): TestServerConfig = TestServerConfig(
    host = getMandatoryEnvVar("TEST_SERVER_HOST"),
    wsPort = getMandatoryEnvVar("TEST_SERVER_WS_PORT").toInt(),
    httpPort = getMandatoryEnvVar("TEST_SERVER_HTTP_PORT").toInt(),
)

private fun getMandatoryEnvVar(varName: String): String =
    process.env[varName] ?: error("Environment variable $varName not provided")

external val process: Process

external interface Process {
    val env: Env
}

external interface Env {
    operator fun get(key: String): String?
}
