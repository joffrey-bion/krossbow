package org.hildan.krossbow.websocket.ktor

internal expect fun extractHandshakeStatusCode(handshakeException: Exception): Int?
