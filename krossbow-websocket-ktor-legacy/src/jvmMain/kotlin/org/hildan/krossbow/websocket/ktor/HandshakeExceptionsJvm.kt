package org.hildan.krossbow.websocket.ktor

import java.net.ProtocolException
import java.net.UnknownHostException
import java.net.http.WebSocketHandshakeException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal actual fun extractHandshakeStatusCode(handshakeException: Exception): Int? = when {
    // no status code if we can't even contact the host
    handshakeException is UnknownHostException -> null
    // with OkHttp engine, we get ProtocolException with itself as cause - we can only parse the message
    handshakeException is ProtocolException -> extractHandshakeStatusCode(handshakeException)
    handshakeException.safeIs<WebSocketHandshakeException>() -> extractHandshakeStatusCode(handshakeException)
    else -> null
}

private val protocolExceptionMessageRegex = Regex("""Expected HTTP 101 response but was '(\d{3}) [^']+'""")

private fun extractHandshakeStatusCode(handshakeException: ProtocolException): Int? {
    val message = handshakeException.message ?: return null
    return protocolExceptionMessageRegex.matchEntire(message)?.groupValues?.get(1)?.toInt()
}

private fun extractHandshakeStatusCode(webSocketHandshakeException: WebSocketHandshakeException) =
    webSocketHandshakeException.response.statusCode()

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
