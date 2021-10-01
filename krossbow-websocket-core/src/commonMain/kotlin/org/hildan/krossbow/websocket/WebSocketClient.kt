package org.hildan.krossbow.websocket

import kotlinx.coroutines.channels.ReceiveChannel

@Deprecated(
    message = "This method pollutes the global namespace and will be removed in a future version. " +
        "Please use WebSocketClient.default() factory function instead",
    replaceWith = ReplaceWith("org.hildan.krossbow.websocket.WebSocketClient.default()"),
)
expect fun defaultWebSocketClient(): WebSocketClient

/**
 * Gets the default [WebSocketClient] implementation for the current platform.
 */
expect fun WebSocketClient.Companion.default(): WebSocketClient

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
     * @throws WebSocketConnectionException if an error occurs during the connection.
     */
    suspend fun connect(url: String): WebSocketConnection

    companion object
}

@Deprecated("This has been renamed to WebSocketConnection", ReplaceWith("WebSocketConnection"))
typealias WebSocketSession = WebSocketConnection

/**
 * Represents a web socket connection to another endpoint.
 *
 * Implementations must be safe to call concurrently.
 */
interface WebSocketConnection {

    /**
     * The URL that was used to connect this web socket.
     */
    val url: String

    /**
     * The host to which this web socket is connected.
     */
    val host: String
        get() = url.substringAfter("://").substringBefore("/").substringBefore(":")

    /**
     * Whether it is safe to send messages through this web socket.
     * If this is false, sending frames should not be attempted and may throw an exception.
     *
     * This is usually based on the underlying web socket implementation "closed for send" status.
     * However, some web socket implementations like OkHttp don't expose their status but always allow calls to their
     * `send` methods (which are turned into no-ops when the web socket is closed).
     * For such implementations, `canSend` always returns `true`.
     */
    val canSend: Boolean

    /**
     * The channel of incoming web socket frames.
     */
    val incomingFrames: ReceiveChannel<WebSocketFrame>

    /**
     * Sends a web socket text frame.
     */
    suspend fun sendText(frameText: String)

    /**
     * Sends a web socket binary frame.
     */
    suspend fun sendBinary(frameData: ByteArray)

    /**
     * Sends a web socket close frame with the given [code] and [reason], and closes the connection.
     *
     * The [code] can be any of the [WebSocketCloseCodes] defined by the specification, except
     * [NO_STATUS_CODE][WebSocketCloseCodes.NO_STATUS_CODE] and [NO_CLOSE_FRAME][WebSocketCloseCodes.NO_CLOSE_FRAME]
     * which are reserved for representing the absence of close code or close frame and should not be sent in a frame
     * (as defined by the specification in
     * [section 7.4.1 of RFC-6455](https://tools.ietf.org/html/rfc6455#section-7.4.1)).
     *
     * The [reason] must not be longer than 123 *bytes* (not characters!) when encoded in UTF-8, due to the limit on
     * control frames defined by the web socket protocol specification
     * [RFC-6455](https://tools.ietf.org/html/rfc6455#section-5.5).
     * You can use [String.truncateToCloseFrameReasonLength] if you don't control the length of the reason and yet still
     * want to avoid exceptions.
     */
    suspend fun close(code: Int = WebSocketCloseCodes.NORMAL_CLOSURE, reason: String? = null)
}

interface WebSocketConnectionWithPingPong : WebSocketConnection {

    /**
     * Sends a web socket ping frame.
     */
    suspend fun sendPing(frameData: ByteArray)

    /**
     * Sends a web socket pong frame.
     *
     * Note that implementations usually take care of sending Pong frames corresponding to each received Ping frame, so
     * applications should not bother dealing with Pongs in general.
     * Unsollicited Pong frames may be sent, however, to serve as unidirectional heart beats.
     */
    suspend fun sendPong(frameData: ByteArray)
}

/**
 * An exception thrown when something went wrong at web socket level.
 */
open class WebSocketException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * An exception thrown when something went wrong during the web socket connection.
 */
open class WebSocketConnectionException(
    val url: String,
    message: String = "Couldn't connect to web socket at $url",
    cause: Throwable? = null
) : WebSocketException(message, cause)

/**
 * An exception thrown when the server closed the connection unexpectedly.
 */
class WebSocketConnectionClosedException(
    url: String,
    val code: Int,
    val reason: String?
) : WebSocketConnectionException(
    url,
    "Couldn't connect to web socket at $url. The server closed the connection. Code: $code Reason: $reason"
)
