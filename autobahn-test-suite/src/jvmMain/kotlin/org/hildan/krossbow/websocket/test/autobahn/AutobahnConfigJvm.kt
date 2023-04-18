package org.hildan.krossbow.websocket.test.autobahn

actual fun getDefaultAutobahnConfig() = AutobahnConfig(
    host = getMandatoryEnvVar("AUTOBAHN_SERVER_HOST"),
    wsPort = getMandatoryEnvVar("AUTOBAHN_SERVER_TCP_9001").toInt(),
    webPort = getMandatoryEnvVar("AUTOBAHN_SERVER_TCP_8080").toInt(),
)

private fun getMandatoryEnvVar(varName: String): String =
    System.getenv(varName) ?: error("Environment variable $varName not provided")
