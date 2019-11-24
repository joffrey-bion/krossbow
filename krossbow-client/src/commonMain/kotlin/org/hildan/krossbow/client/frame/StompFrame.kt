package org.hildan.krossbow.client.frame

import org.hildan.krossbow.client.headers.StompConnectHeaders
import org.hildan.krossbow.client.headers.StompConnectedHeaders
import org.hildan.krossbow.client.headers.StompDisconnectHeaders
import org.hildan.krossbow.client.headers.StompHeaders
import org.hildan.krossbow.client.headers.StompSendHeaders

object StompCommands {
    const val CONNECT = "CONNECT"
    const val CONNECTED = "CONNECTED"
    const val SEND = "SEND"
    const val SUBSCRIBE = "SUBSCRIBE"
    const val UNSUBSCRIBE = "UNSUBSCRIBE"
    const val ACK = "ACK"
    const val NACK = "NACK"
    const val BEGIN = "BEGIN"
    const val COMMIT = "COMMIT"
    const val ABORT = "ABORT"
    const val DISCONNECT = "DISCONNECT"
    const val MESSAGE = "MESSAGE"
    const val RECEIPT = "RECEIPT"
    const val ERROR = "ERROR"
}

sealed class StompFrame(
    val command: String,
    open val headers: StompHeaders,
    open val body: String? = null
) {
    data class Connect(override val headers: StompConnectHeaders) : StompFrame(
        StompCommands.CONNECT, headers)

    data class Connected(override val headers: StompConnectedHeaders) : StompFrame(
        StompCommands.CONNECTED, headers)

    data class Send(
        override val headers: StompSendHeaders,
        override val body: String?
    ) : StompFrame(StompCommands.SEND, headers, body)

    data class Disconnect(override val headers: StompDisconnectHeaders) : StompFrame(
        StompCommands.DISCONNECT, headers)

    fun toBytes(): ByteArray {
        TODO()
    }
}

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
