package org.hildan.krossbow.stomp

import org.hildan.krossbow.stomp.frame.StompFrame
import kotlin.time.Duration

/**
 * An exception thrown when a STOMP ERROR frame is received.
 * It is usually thrown through subscription channels.
 */
class StompErrorFrameReceived(val frame: StompFrame.Error) : Exception(frame.message)

/**
 * An exception thrown when a RECEIPT frame was expected from the server, but not received in the configured time limit.
 */
class LostReceiptException(
    /** The value of the receipt header sent to the server, and expected in a RECEIPT frame. */
    val receiptId: String,
    /** The configured timeout which has expired. */
    val configuredTimeout: Duration,
    /** The frame which did not get acknowledged by the server. */
    val frame: StompFrame,
) : Exception("No RECEIPT frame received for receiptId '$receiptId' (in ${frame.command} frame) within $configuredTimeout")

/**
 * An exception thrown when expected heart beats are not received.
 */
class MissingHeartBeatException(
    val expectedPeriod: Duration,
) : Exception("A server heart beat was missing (expecting data every $expectedPeriod at most)")

/**
 * An exception thrown when the underlying websocket connection was closed at an inappropriate time.
 */
class WebSocketClosedUnexpectedly(
    val code: Int,
    val reason: String?,
) : Exception("the WebSocket was closed while subscriptions were still active. Code: $code Reason: $reason")

/**
 * An exception thrown when the STOMP frames flow completed while some consumer was expecting more frames.
 */
class SessionDisconnectedException(message: String) : Exception(message)
