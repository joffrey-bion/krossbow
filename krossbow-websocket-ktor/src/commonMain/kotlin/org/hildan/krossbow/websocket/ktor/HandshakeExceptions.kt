package org.hildan.krossbow.websocket.ktor

import io.ktor.client.plugins.websocket.*

internal data class HandshakeFailureDetails(val statusCode: Int?, val additionalInfo: String?)

// This is the message for invalid status codes on CIO engine
private val wrongStatusExceptionMessageRegex = Regex("""Handshake exception, expected status code 101 but was (\d{3})""")

internal fun extractKtorHandshakeFailureDetails(handshakeException: WebSocketException): HandshakeFailureDetails {
    val message = handshakeException.message ?: return extractHandshakeFailureDetails(handshakeException)
    val match = wrongStatusExceptionMessageRegex.matchEntire(message)
    return HandshakeFailureDetails(
        statusCode = match?.groupValues?.get(1)?.toInt(),
        additionalInfo = message,
    )
}

internal expect fun extractHandshakeFailureDetails(handshakeException: Exception): HandshakeFailureDetails

internal fun genericFailureDetails(handshakeException: Exception) = HandshakeFailureDetails(
    statusCode = null,
    additionalInfo = handshakeException.toString(), // not only the message because the exception name is useful
)
