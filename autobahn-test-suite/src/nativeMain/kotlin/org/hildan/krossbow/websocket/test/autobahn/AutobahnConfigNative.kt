package org.hildan.krossbow.websocket.test.autobahn

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

actual fun getDefaultAutobahnConfig() = AutobahnConfig(
    host = getMandatoryEnvVar("AUTOBAHN_SERVER_HOST"),
    wsPort = getMandatoryEnvVar("AUTOBAHN_SERVER_TCP_9001").toInt(),
    webPort = getMandatoryEnvVar("AUTOBAHN_SERVER_TCP_8080").toInt(),
)

@OptIn(ExperimentalForeignApi::class)
private fun getMandatoryEnvVar(varName: String): String = getenv(varName)?.toKString()
    ?: error("Environment variable $varName not provided")
