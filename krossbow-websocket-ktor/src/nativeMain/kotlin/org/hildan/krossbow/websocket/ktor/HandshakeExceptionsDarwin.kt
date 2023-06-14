package org.hildan.krossbow.websocket.ktor

// TODO find out the exception for native platforms?
internal actual fun extractHandshakeFailureDetails(handshakeException: Exception): HandshakeFailureDetails =
    genericFailureDetails(handshakeException)
