package org.hildan.krossbow.engines

/**
 * Configuration of the STOMP [KrossbowClient].
 */
data class KrossbowConfig(
    /**
     * The heartbeat to use during STOMP sessions.
     */
    var heartBeat: HeartBeat = HeartBeat(),
    val autoReceipt: Boolean = false,
    val receiptTimeLimit: Long = 15000
)

/**
 * Defines the heart beats for STOMP sessions, as specified in the STOMP specification.
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
