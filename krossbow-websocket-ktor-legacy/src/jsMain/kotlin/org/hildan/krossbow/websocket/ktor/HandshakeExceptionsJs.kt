package org.hildan.krossbow.websocket.ktor

// "unnecessary" escapes are necessary on JS (https://youtrack.jetbrains.com/issue/KTIJ-19147)
@Suppress("RegExpRedundantEscape")
private val jsonExceptionMessageRegex = Regex("""\{"message":"(.*?)","target":\{\},"type":"error"\}""")

private val unexpectedServerResponseMessageRegex = Regex("""Unexpected server response: (\d{3})""")

internal actual fun extractHandshakeFailureDetails(handshakeException: Exception): HandshakeFailureDetails {
    val json = handshakeException.message ?: return genericFailureDetails(handshakeException)
    // we use regex instead of eval() for security reasons
    val actualMessage = jsonExceptionMessageRegex.matchEntire(json)?.groupValues?.get(1)
        ?: return HandshakeFailureDetails(statusCode = null, additionalInfo = json)

    val match = unexpectedServerResponseMessageRegex.matchEntire(actualMessage)
        ?: return HandshakeFailureDetails(statusCode = null, additionalInfo = actualMessage)

    return HandshakeFailureDetails(
        statusCode = match.groupValues[1].toInt(),
        additionalInfo = actualMessage,
    )
}
