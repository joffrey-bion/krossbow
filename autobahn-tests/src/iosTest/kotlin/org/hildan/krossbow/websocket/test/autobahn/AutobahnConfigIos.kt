package org.hildan.krossbow.websocket.test.autobahn

import kotlinx.cinterop.toKString
import platform.posix.getenv

actual fun getDefaultAutobahnTestServerHost(): String = getenv("AUTOBAHN_SERVER_HOST")?.toKString()
    ?: error("Environment variable AUTOBAHN_SERVER_HOST not provided")

actual fun getDefaultAutobahnTestServerPort(): Int = getenv("AUTOBAHN_SERVER_TCP_9001")?.toKString()?.toInt()
    ?: error("Environment variable AUTOBAHN_SERVER_TCP_9001 not provided")
