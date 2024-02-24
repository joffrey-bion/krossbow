package org.hildan.krossbow.websocket.ktor

import java.net.ProtocolException
import java.net.UnknownHostException
import java.net.http.WebSocketHandshakeException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal actual fun extractHandshakeFailureDetails(handshakeException: Exception): HandshakeFailureDetails = when {
    // no status code if we can't even contact the host
    handshakeException is UnknownHostException -> genericFailureDetails(handshakeException)
    // with OkHttp engine, we get ProtocolException with itself as cause - we can only parse the message
    handshakeException is ProtocolException -> extractHandshakeFailureDetails(handshakeException)
    handshakeException.safeIs<WebSocketHandshakeException>() -> extractHandshakeFailureDetails(handshakeException)
    else -> genericFailureDetails(handshakeException)
}

private const val okhttp407WithoutProxyMessage = "Received HTTP_PROXY_AUTH (407) code while not using proxy"
private val okhttpInvalidStatusCodeMessageRegex = Regex("""Expected HTTP 101 response but was '(\d{3}) ([^']*)'""")

private fun extractHandshakeFailureDetails(handshakeException: ProtocolException): HandshakeFailureDetails {
    val message = handshakeException.message ?: return genericFailureDetails(handshakeException)
    if (message == okhttp407WithoutProxyMessage) {
        return HandshakeFailureDetails(statusCode = 407, additionalInfo = message)
    }
    val match = okhttpInvalidStatusCodeMessageRegex.matchEntire(message)
    return HandshakeFailureDetails(
        statusCode = match?.groupValues?.get(1)?.toInt(),
        additionalInfo = (match?.groupValues?.get(2) ?: handshakeException.message)?.takeIf { it.isNotBlank() },
    )
}

private fun extractHandshakeFailureDetails(webSocketHandshakeException: WebSocketHandshakeException) =
    HandshakeFailureDetails(
        statusCode = webSocketHandshakeException.response.statusCode(),
        additionalInfo = (webSocketHandshakeException.response.body() as? String)?.takeIf { it.isNotBlank() },
    )

/**
 * Returns true if [C] is on the classpath and `this` is an instance of [C].
 *
 * Doesn't fail with [NoClassDefFoundError] if [C] is not present.
 */
@OptIn(ExperimentalContracts::class)
private inline fun <reified C : Any> Any.safeIs(): Boolean {
    contract {
        returns(true) implies(this@safeIs is C)
    }
    return try {
        this is C
    } catch (e: NoClassDefFoundError) {
        false
    }
}
