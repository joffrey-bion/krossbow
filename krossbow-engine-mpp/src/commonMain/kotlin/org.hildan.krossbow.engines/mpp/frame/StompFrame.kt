package org.hildan.krossbow.engines.mpp.frame

import org.hildan.krossbow.engines.mpp.headers.StompConnectHeaders
import org.hildan.krossbow.engines.mpp.headers.StompConnectedHeaders
import org.hildan.krossbow.engines.mpp.headers.StompDisconnectHeaders
import org.hildan.krossbow.engines.mpp.headers.StompErrorHeaders
import org.hildan.krossbow.engines.mpp.headers.StompHeaders
import org.hildan.krossbow.engines.mpp.headers.StompMessageHeaders
import org.hildan.krossbow.engines.mpp.headers.StompReceiptHeaders
import org.hildan.krossbow.engines.mpp.headers.StompSendHeaders
import org.hildan.krossbow.engines.mpp.headers.StompSubscribeHeaders
import org.hildan.krossbow.engines.mpp.headers.StompUnsubscribeHeaders

enum class StompCommand(
    val text: String,
    val supportsHeaderEscapes: Boolean = true
) {
    // The CONNECT and CONNECTED frames do not escape the carriage return, line feed or colon octets
    // in order to remain backward compatible with STOMP 1.0
    // https://stomp.github.io/stomp-specification-1.2.html#Value_Encoding
    CONNECT("CONNECT", supportsHeaderEscapes = false),
    CONNECTED("CONNECTED", supportsHeaderEscapes = false),
    SEND("SEND"),
    SUBSCRIBE("SUBSCRIBE"),
    UNSUBSCRIBE("UNSUBSCRIBE"),
    ACK("ACK"),
    NACK("NACK"),
    BEGIN("BEGIN"),
    COMMIT("COMMIT"),
    ABORT("ABORT"),
    DISCONNECT("DISCONNECT"),
    MESSAGE("MESSAGE"),
    RECEIPT("RECEIPT"),
    ERROR("ERROR");

    companion object {
        private val valuesByText = values().associateBy { it.text }

        fun parse(text: String) = valuesByText[text] ?: error("Unknown STOMP command $text")
    }
}

sealed class StompFrame(
    val command: StompCommand,
    open val headers: StompHeaders,
    open val body: FrameBody? = null
) {
    data class Connect(override val headers: StompConnectHeaders) : StompFrame(StompCommand.CONNECT, headers)

    data class Connected(override val headers: StompConnectedHeaders) : StompFrame(StompCommand.CONNECTED, headers)

    data class Subscribe(override val headers: StompSubscribeHeaders) : StompFrame(StompCommand.SUBSCRIBE, headers)

    data class Unsubscribe(override val headers: StompUnsubscribeHeaders) :
        StompFrame(StompCommand.UNSUBSCRIBE, headers)

    data class Send(
        override val headers: StompSendHeaders,
        override val body: FrameBody?
    ) : StompFrame(StompCommand.SEND, headers, body)

    data class Message(
        override val headers: StompMessageHeaders,
        override val body: FrameBody?
    ) : StompFrame(StompCommand.MESSAGE, headers, body)

    data class Receipt(override val headers: StompReceiptHeaders) : StompFrame(StompCommand.RECEIPT, headers)

    data class Disconnect(override val headers: StompDisconnectHeaders) : StompFrame(StompCommand.DISCONNECT, headers)

    data class Error(
        override val headers: StompErrorHeaders,
        override val body: FrameBody?
    ) : StompFrame(StompCommand.MESSAGE, headers, body) {
        val message: String = headers.message ?: (body as? FrameBody.Text)?.text ?: "(binary error message)"
    }

    @UseExperimental(ExperimentalStdlibApi::class)
    fun toBytes(): ByteArray {
        // TODO make this more efficient
        return format().encodeToByteArray()
    }
}

sealed class FrameBody(open val rawBytes: ByteArray) {

    @UseExperimental(ExperimentalStdlibApi::class)
    data class Text(val text: String) : FrameBody(text.encodeToByteArray())

    data class Binary(override val rawBytes: ByteArray) : FrameBody(rawBytes) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Binary

            if (!rawBytes.contentEquals(other.rawBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            return rawBytes.contentHashCode()
        }
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
