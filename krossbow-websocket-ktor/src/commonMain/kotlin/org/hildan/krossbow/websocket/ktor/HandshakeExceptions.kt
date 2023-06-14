package org.hildan.krossbow.websocket.ktor

internal data class HandshakeFailureDetails(val statusCode: Int?, val additionalInfo: String?)

internal expect fun extractHandshakeFailureDetails(handshakeException: Exception): HandshakeFailureDetails

internal fun genericFailureDetails(handshakeException: Exception) = HandshakeFailureDetails(
    statusCode = null,
    additionalInfo = handshakeException.toString(), // not only the message because the exception name is useful
)
