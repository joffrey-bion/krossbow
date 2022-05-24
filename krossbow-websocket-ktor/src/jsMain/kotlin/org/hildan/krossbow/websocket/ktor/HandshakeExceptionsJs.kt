package org.hildan.krossbow.websocket.ktor

// "unnecessary" escapes are necessary on JS (https://youtrack.jetbrains.com/issue/KTIJ-19147)
@Suppress("RegExpRedundantEscape")
private val jsonExceptionMessageRegex = Regex("""\{"message":"(.*?)","target":\{\},"type":"error"\}""")

private val unexpectedServerResponseMessageRegex = Regex("""Unexpected server response: (\d{3})""")

internal actual fun extractHandshakeStatusCode(handshakeException: Exception): Int? {
    val json = handshakeException.message ?: return null
    // we use regex instead of eval() for security reasons
    val actualMessage = jsonExceptionMessageRegex.matchEntire(json)?.groupValues?.get(1) ?: return null
    return unexpectedServerResponseMessageRegex.matchEntire(actualMessage)?.groupValues?.get(1)?.toInt()
}
