package org.hildan.krossbow.stomp.config

import org.hildan.krossbow.converters.KotlinxSerialization
import org.hildan.krossbow.converters.MessageConverter

/**
 * Configuration for the STOMP protocol.
 */
data class StompConfig(
    /**
     * The [HeartBeat] to use during STOMP sessions.
     */
    var heartBeat: HeartBeat = HeartBeat(),
    /**
     * Whether to automatically attach a `receipt` header to the sent messages in order to track receipts.
     */
    var autoReceipt: Boolean = false,
    /**
     * As suggested by the specification, when disconnecting from the server, the client should first send a
     * DISCONNECT frame with a `receipt` header, and then wait for a RECEIPT frame before closing the connection.
     * If this graceful shutdown is disabled, then there is no guarantee that the server received all previous messages.
     * More info: https://stomp.github.io/stomp-specification-1.2.html#DISCONNECT
     */
    var gracefulDisconnect: Boolean = true,
    /**
     * Defines how long to wait for a RECEIPT frame from the server before throwing an exception.
     * Only crashes when a `receipt` header was actually present in the sent frame (and thus a RECEIPT was expected).
     * Such header is always present if [autoReceipt] is enabled.
     */
    var receiptTimeLimit: Long = 15000,
    /**
     * Used for conversion of message payloads to Kotlin objects. Defaults to JSON conversion using Kotlinx
     * Serialization.
     */
    var messageConverter: MessageConverter = KotlinxSerialization.JsonConverter()
)

/**
 * Defines the heart beats for STOMP sessions, as specified in the STOMP specification.
 *
 * More info: https://stomp.github.io/stomp-specification-1.2.html#Heart-beating
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
