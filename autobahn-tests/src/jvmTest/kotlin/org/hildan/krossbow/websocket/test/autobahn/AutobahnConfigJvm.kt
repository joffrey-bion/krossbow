package org.hildan.krossbow.websocket.test.autobahn

actual fun getDefaultAutobahnTestServerHost(): String =
    System.getenv("AUTOBAHN_SERVER_HOST") ?: error("Environment variable AUTOBAHN_SERVER_HOST not provided")

actual fun getDefaultAutobahnTestServerPort(): Int =
    System.getenv("AUTOBAHN_SERVER_TCP_9001")?.toInt() ?: error("Environment variable AUTOBAHN_SERVER_TCP_9001 not provided")
