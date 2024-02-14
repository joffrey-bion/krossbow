package org.hildan.krossbow.websocket

import kotlinx.coroutines.flow.*
import kotlinx.io.bytestring.*

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
     * If false, sending frames should not be attempted and will likely throw an exception.
     * If true, sending frames will likely succeed.
     * However, no guarantees can be made because there could be a race condition between WS closure and a "send" call.
     *
     * This is usually based on the underlying web socket implementation "closed for send" status.
     * However, some web socket implementations like OkHttp or iOS don't expose their status, and `canSend` always returns `true`.
     * Note that OkHttp always allows calls to its `send` methods (which are turned into no-ops when the web socket is closed).
     */
    val canSend: Boolean

    /**
     * The single-consumer hot flow of incoming web socket frames.
     *
     * This flow is designed for a single-consumer.
     * If multiple collectors collect it at the same time, each frame will go to only one of them in a fan-out manner.
     * For a broadcast behaviour, use [Flow.shareIn] to convert this flow into a [SharedFlow].
     *
     * This flow is hot, meaning that the frames are coming and are buffered even when there is no collector.
     * This means that it's ok to have a delay between the connection and the collection of [incomingFrames], no
     * frames will be lost.
     * It's also ok to stop collecting the flow, and start again later: the frames that are received in the meantime
     * are buffered and sent to the collector when it comes back.
     */
    val incomingFrames: Flow<WebSocketFrame>

    /**
     * Sends a web socket text frame.
     *
     * This method suspends until the underlying web socket implementation has processed the message.
     * Some implementations don't provide any ways to track when exactly the message is sent.
     * For those implementations, this method returns immediately without suspending.
     */
    suspend fun sendText(frameText: String)

    /**
     * Sends a web socket binary frame.
     *
     * This method suspends until the underlying web socket implementation has processed the message.
     * Some implementations don't provide any ways to track when exactly the message is sent.
     * For those implementations, this method returns immediately without suspending.
     */
    suspend fun sendBinary(frameData: ByteString)

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

interface WebSocketConnectionWithPing : WebSocketConnection {

    /**
     * Sends a web socket ping frame.
     */
    suspend fun sendPing(frameData: ByteString)
}

interface WebSocketConnectionWithPingPong : WebSocketConnectionWithPing {

    /**
     * Sends an unsolicited web socket pong frame.
     *
     * Note that implementations usually take care of sending Pong frames corresponding to each received Ping frame, so
     * applications should not bother dealing with Pongs in general.
     * Unsolicited Pong frames may be sent, however, for instance to serve as unidirectional heart beats.
     */
    suspend fun sendPong(frameData: ByteString)
}
