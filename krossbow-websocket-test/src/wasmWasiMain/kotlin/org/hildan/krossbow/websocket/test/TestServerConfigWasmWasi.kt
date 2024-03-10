package org.hildan.krossbow.websocket.test

actual fun getTestServerConfig(): TestServerConfig = getTestServerConfigFromEnv()

private fun getTestServerConfigFromEnv(): TestServerConfig = TestServerConfig(
    host = getMandatoryEnvVar("TEST_SERVER_HOST"),
    wsPort = getMandatoryEnvVar("TEST_SERVER_WS_PORT").toInt(),
    httpPort = getMandatoryEnvVar("TEST_SERVER_HTTP_PORT").toInt(),
)

private fun getMandatoryEnvVar(varName: String): String =
    envVars[varName] ?: error("Environment variable $varName not provided")

private val envVars: Map<String, String> by lazy { environGet() }