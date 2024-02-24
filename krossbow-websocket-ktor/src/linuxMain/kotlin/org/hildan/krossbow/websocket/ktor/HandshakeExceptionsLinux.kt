package org.hildan.krossbow.websocket.ktor

// TODO find out the exception for linux platform on non-CIO engines
internal actual fun extractHandshakeFailureDetails(handshakeException: Exception): HandshakeFailureDetails =
    genericFailureDetails(handshakeException)
