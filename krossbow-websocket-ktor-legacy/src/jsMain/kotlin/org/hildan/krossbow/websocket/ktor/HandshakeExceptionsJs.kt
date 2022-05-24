package org.hildan.krossbow.websocket.ktor

internal actual fun extractHandshakeStatusCode(handshakeException: Exception): Int? {
    // There is no status code information in the exceptions thrown in Ktor 1,
    // the exception is generic (WebSocketException) and its message is empty
    return null
}
