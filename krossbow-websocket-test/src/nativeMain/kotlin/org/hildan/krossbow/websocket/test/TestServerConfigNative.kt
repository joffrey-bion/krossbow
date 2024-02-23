package org.hildan.krossbow.websocket.test

import kotlinx.cinterop.*
import platform.posix.*

actual fun getTestServerConfig() = TestServerConfig(
    host = getMandatoryEnvVar("TEST_SERVER_HOST"),
    wsPort = getMandatoryEnvVar("TEST_SERVER_WS_PORT").toInt(),
    httpPort = getMandatoryEnvVar("TEST_SERVER_HTTP_PORT").toInt(),
)

@OptIn(ExperimentalForeignApi::class)
private fun getMandatoryEnvVar(varName: String): String = getenv(varName)?.toKString()
    ?: error("Environment variable $varName not provided")
