package org.hildan.krossbow.websocket

import kotlinx.coroutines.channels.ReceiveChannel

/**
 * A web socket client.
 *
 * The client is used to connect to the server and create a [WebSocketSession].
 * Then, most of the interactions are done through the [WebSocketSession] until it is [closed][WebSocketSession.close].
 *
 * The same client can be reused to start multiple sessions, unless specified otherwise by the implementation.
 */
interface WebSocketClient {

    /**
     * Opens a web socket connection and suspends until the connection is OPEN.
     */
    suspend fun connect(url: String): WebSocketSession
}

/**
 * A session to interact with another endpoint via a web socket.
 */
interface WebSocketSession {

    val incomingFrames: ReceiveChannel<WebSocketFrame>

    suspend fun sendText(frameText: String)

    suspend fun sendBinary(frameData: ByteArray)

    suspend fun close(code: Int = WebSocketCloseCodes.NORMAL_CLOSURE, reason: String? = null)
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
) : WebSocketConnectionException(url,
    "Couldn't connect to web socket at $url. The server closed the connection. Code: $code Reason: $reason"
)
