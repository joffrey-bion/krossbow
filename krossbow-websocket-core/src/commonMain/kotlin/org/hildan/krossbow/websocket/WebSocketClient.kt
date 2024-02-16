package org.hildan.krossbow.websocket

/**
 * A web socket client.
 *
 * The client is used to connect to the server and create a [WebSocketConnection].
 * Then, most of the interactions are done through the [WebSocketConnection] until it is [closed][WebSocketConnection.close].
 *
 * The same client can be reused to start multiple sessions, unless specified otherwise by the implementation.
 */
interface WebSocketClient {

    /**
     * Opens a web socket connection and suspends until the connection is OPEN.
     *
     * @param headers custom headers to send during the web socket handshake. Support for custom handshake headers is
     * optional. Implementations that don't support them must throw an [IllegalArgumentException] if [headers] is not
     * empty.
     *
     * @throws WebSocketConnectionException if an error occurs during the connection.
     */
    suspend fun connect(url: String, headers: Map<String, String> = emptyMap()): WebSocketConnection

    companion object
}

/**
 * An exception thrown when something went wrong at web socket level.
 */
open class WebSocketException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * An exception thrown when something went wrong during the web socket connection.
 */
open class WebSocketConnectionException(
    /** The target URL of the failed connection. */
    val url: String,
    /** The status code in the HTTP response from the handshake request, if available. */
    val httpStatusCode: Int? = null,
    val additionalInfo: String? = null,
    message: String = defaultMessage(url, httpStatusCode, additionalInfo),
    cause: Throwable? = null,
) : WebSocketException(message, cause)

private fun defaultMessage(url: String, httpStatusCode: Int?, additionalInfo: String?): String {
    val details = details(httpStatusCode, additionalInfo)
    return "Couldn't connect to web socket at $url ($details)"
}

private fun details(httpStatusCode: Int?, additionalInfo: String?): String = when {
    httpStatusCode == null -> additionalInfo ?: "no additional details"
    additionalInfo == null -> "HTTP $httpStatusCode"
    else -> "HTTP $httpStatusCode: $additionalInfo"
}

/**
 * An exception thrown when the server closed the connection unexpectedly during the handshake.
 */
class WebSocketConnectionClosedException(
    url: String,
    val code: Int,
    val reason: String?
) : WebSocketConnectionException(
    url = url,
    message = "Couldn't connect to web socket at $url. The server closed the connection. Code: $code Reason: $reason",
)
