package org.hildan.krossbow.websocket.ktor

import java.net.ProtocolException
import java.net.UnknownHostException
import java.net.http.WebSocketHandshakeException

internal actual fun extractHandshakeStatusCode(handshakeException: Exception): Int? = when (handshakeException) {
    is WebSocketHandshakeException -> handshakeException.response.statusCode()
    // with OkHttp engine, we get ProtocolException with itself as cause - we can only parse the message
    is ProtocolException -> extractHandshakeStatusCode(handshakeException)
    is UnknownHostException -> null
    else -> null
}

private val protocolExceptionMessageRegex = Regex("""Expected HTTP 101 response but was '(\d{3}) [^']+'""")

private fun extractHandshakeStatusCode(handshakeException: ProtocolException): Int? {
    val message = handshakeException.message ?: return null
    return protocolExceptionMessageRegex.matchEntire(message)?.groupValues?.get(1)?.toInt()
}
