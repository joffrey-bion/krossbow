package org.hildan.krossbow.stomp

import org.hildan.krossbow.stomp.frame.StompFrame

/**
 * An exception thrown when a STOMP ERROR frame is received.
 * It is usually thrown through subscription channels.
 */
class StompErrorFrameReceived(val frame: StompFrame.Error) : Exception("STOMP ERROR frame received: ${frame.message}")

/**
 * An exception thrown when a RECEIPT frame was expected from the server, but not received in the configured time limit.
 */
class LostReceiptException(
    /** The value of the receipt header sent to the server, and expected in a RECEIPT frame. */
    val receiptId: String,
    /** The configured timeout which has expired. */
    val configuredTimeoutMillis: Long,
    /** The frame which did not get acknowledged by the server. */
    val frame: StompFrame
) : Exception("No RECEIPT frame received for receiptId '$receiptId' (in ${frame.command} frame) within ${configuredTimeoutMillis}ms")

/**
 * An exception thrown when a MESSAGE frame's body failed to be converted.
 */
class MessageConversionException(cause: Throwable) : Exception(cause.message, cause)

/**
 * An exception thrown when expected heart beats are not received.
 */
class MissingHeartBeatException(
    val expectedPeriodMillis: Int
) : Exception("A server heart beat was missing (expecting data every ${expectedPeriodMillis}ms at most)")

/**
 * An exception thrown when the underlying websocket connection was closed at an inappropriate time.
 */
class WebSocketClosedUnexpectedly(
    val code: Int,
    val reason: String?
) : Exception("the WebSocket was closed while subscriptions were still active. Code: $code Reason: $reason")
