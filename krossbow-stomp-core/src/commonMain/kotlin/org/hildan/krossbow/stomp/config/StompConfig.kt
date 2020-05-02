package org.hildan.krossbow.stomp.config

import org.hildan.krossbow.stomp.LostReceiptException
import org.hildan.krossbow.stomp.StompSession

/**
 * Configuration for the STOMP protocol.
 */
data class StompConfig(
    /**
     * Whether to automatically attach a `receipt` header to the sent messages in order to track receipts.
     */
    var autoReceipt: Boolean = false,
    /**
     * Whether to automatically compute and add the `content-length` header in sent frames.
     */
    var autoContentLength: Boolean = true,
    /**
     * Whether to use the `STOMP` command instead of `CONNECT` to establish the connection.
     *
     * Clients that use the `STOMP` frame instead of the `CONNECT` frame will only be able to connect to STOMP 1.2
     * servers (as well as some STOMP 1.1 servers) but the advantage is that a protocol sniffer/discriminator will be
     * able to differentiate the STOMP connection from an HTTP connection.
     */
    var connectWithStompCommand: Boolean = false,
    /**
     * The [HeartBeat] to request for the STOMP sessions.
     *
     * This is part of a negotiation as defined by the specification, and does not imply that this exact heart beat
     * configuration will be used.
     * The actual heart beats to use will be defined by the CONNECTED frame received from the server as a result of
     * the negotiation.
     */
    var heartBeat: HeartBeat = HeartBeat(),
    /**
     * Defines how long to wait for the websocket+STOMP connection to be established before throwing an exception.
     */
    var connectionTimeoutMillis: Long = 15000,
    /**
     * Defines how long to wait for a RECEIPT frame from the server before throwing a [LostReceiptException].
     * Only crashes when a `receipt` header was actually present in the sent frame (and thus a RECEIPT was expected).
     * Such header is always present if [autoReceipt] is enabled.
     * Note that this doesn't apply to the DISCONNECT frames, use [disconnectTimeoutMillis] instead for that.
     */
    var receiptTimeoutMillis: Long = 500,
    /**
     * Like [receiptTimeoutMillis] but only for the DISCONNECT frame.
     * This is ignored if [gracefulDisconnect] is disabled.
     * Note that if this timeout expires, the [StompSession.disconnect] call doesn't throw an exception.
     * This is to allow servers to close the connection quickly (sometimes too quick for sending a RECEIPT/ERROR) as
     * [mentioned in the specification](http://stomp.github.io/stomp-specification-1.2.html#DISCONNECT).
     */
    var disconnectTimeoutMillis: Long = 200,
    /**
     * Enables [graceful disconnect](https://stomp.github.io/stomp-specification-1.2.html#DISCONNECT):
     * when disconnecting from the server, the client should first send a DISCONNECT frame with a `receipt` header,
     * and then wait for a RECEIPT frame before closing the connection.
     * If this graceful disconnect is disabled, then calling [StompSession.disconnect] immediately closes the web
     * socket connection.
     * In this case, there is no guarantee that the server received all previous messages.
     */
    var gracefulDisconnect: Boolean = true
)

/**
 * Defines the heart beats for STOMP sessions, as
 * [defined in the STOMP specification](https://stomp.github.io/stomp-specification-1.2.html#Heart-beating).
 */
data class HeartBeat(
    /**
     * Represents what the sender of the frame can do (outgoing heart-beats).
     * The value 0 means it cannot send heart-beats, otherwise it is the smallest number of milliseconds between
     * heart-beats that it can guarantee.
     */
    val minSendPeriodMillis: Int = 0,
    /**
     * Represents what the sender of the frame would like to get (incoming heart-beats).
     * The value 0 means it does not want to receive heart-beats, otherwise it is the desired number of milliseconds
     * between heart-beats.
     */
    val expectedPeriodMillis: Int = 0
)
