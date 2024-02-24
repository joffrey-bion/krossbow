package org.hildan.krossbow.websocket.ktor

internal actual fun extractHandshakeFailureDetails(handshakeException: Exception): HandshakeFailureDetails =
    when (handshakeException) {
        is IllegalStateException -> extractHandshakeFailureDetails(handshakeException)
        else -> genericFailureDetails(handshakeException)
    }

/**
 * Strips out the exception name because IllegalStateException is not useful info.
 *
 * We keep the original message because there is nothing more we can do:
 *  * Same message for any invalid response status code:
 *    `Unable to upgrade websocket: The operation identifier is not valid. Error 4317 (0x800710dd)`
 *  * Message for unresolved host:
 *    `Failed to send request: The server name or address could not be resolved. Error 12007 (0x80072ee7)`
 */
private fun extractHandshakeFailureDetails(exception: IllegalStateException): HandshakeFailureDetails =
    HandshakeFailureDetails(statusCode = null, additionalInfo = exception.message)
