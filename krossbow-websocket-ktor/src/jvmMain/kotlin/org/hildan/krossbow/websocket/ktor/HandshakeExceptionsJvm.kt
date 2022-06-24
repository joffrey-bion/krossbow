package org.hildan.krossbow.websocket.ktor

import java.net.ProtocolException
import java.net.UnknownHostException
import java.net.http.WebSocketHandshakeException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal actual fun extractHandshakeStatusCode(handshakeException: Exception): Int? {
    val e = handshakeException
    return when {
        // no status code if we can't even contact the host
        e is UnknownHostException -> null
        // with OkHttp engine, we get ProtocolException with itself as cause - we can only parse the message
        e is ProtocolException -> extractHandshakeStatusCode(e)
        e.safeIs<WebSocketHandshakeException>("java.net.http.WebSocketHandshakeException") -> {
            extractHandshakeStatusCode(e)
        }
        else -> null
    }
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
 * The given [className] must match the fully qualified name of [C].
 *
 * Doesn't fail with [NoClassDefFoundError] if [C] is not present.
 */
@OptIn(ExperimentalContracts::class)
private inline fun <reified C : Any> Any.safeIs(className: String): Boolean {
    contract {
        returns(true) implies(this@safeIs is C)
    }
    if (!classExists(className)) {
        return false
    }
    checkMatches<C>(className) // prevent developer mistakes
    return this is C
}

private inline fun <reified T : Any> checkMatches(className: String) {
    val typeName = T::class.qualifiedName
    require(typeName == className) {
        "Mismatch between the given class name '$className' and the actual parameter type of the action lambda '$typeName"
    }
}

private fun classExists(className: String): Boolean = try {
    Class.forName(className)
    true
} catch (e: ClassNotFoundException) {
    false
}
