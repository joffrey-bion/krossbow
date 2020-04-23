package org.hildan.krossbow.stomp.frame

import org.hildan.krossbow.stomp.headers.StompAbortHeaders
import org.hildan.krossbow.stomp.headers.StompAckHeaders
import org.hildan.krossbow.stomp.headers.StompBeginHeaders
import org.hildan.krossbow.stomp.headers.StompCommitHeaders
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.stomp.headers.StompDisconnectHeaders
import org.hildan.krossbow.stomp.headers.StompErrorHeaders
import org.hildan.krossbow.stomp.headers.StompHeaders
import org.hildan.krossbow.stomp.headers.StompMessageHeaders
import org.hildan.krossbow.stomp.headers.StompNackHeaders
import org.hildan.krossbow.stomp.headers.StompReceiptHeaders
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.stomp.headers.StompUnsubscribeHeaders

enum class StompCommand(
    val text: String,
    val supportsHeaderEscapes: Boolean = true
) {
    STOMP("STOMP"),
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

        fun parse(text: String) = valuesByText[text] ?: throw InvalidStompCommandException("Unknown STOMP command $text")
    }
}

class InvalidStompCommandException(val invalidText: String) : Exception("Unknown STOMP command '$invalidText'")

sealed class StompFrame(
    val command: StompCommand,
    open val headers: StompHeaders,
    open val body: FrameBody? = null
) {
    data class Stomp(override val headers: StompConnectHeaders) : StompFrame(StompCommand.STOMP, headers)

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

    data class Ack(override val headers: StompAckHeaders) : StompFrame(StompCommand.ACK, headers)

    data class Nack(override val headers: StompNackHeaders) : StompFrame(StompCommand.NACK, headers)

    data class Begin(override val headers: StompBeginHeaders) : StompFrame(StompCommand.BEGIN, headers)

    data class Commit(override val headers: StompCommitHeaders) : StompFrame(StompCommand.COMMIT, headers)

    data class Abort(override val headers: StompAbortHeaders) : StompFrame(StompCommand.ABORT, headers)

    data class Disconnect(override val headers: StompDisconnectHeaders) : StompFrame(StompCommand.DISCONNECT, headers)

    data class Error(
        override val headers: StompErrorHeaders,
        override val body: FrameBody?
    ) : StompFrame(StompCommand.ERROR, headers, body) {
        val message: String = headers.message ?: (body as? FrameBody.Text)?.text ?: "(binary error message)"
    }

    companion object {

        fun create(
            command: StompCommand,
            headers: StompHeaders,
            body: FrameBody?
        ): StompFrame = when (command) {
            StompCommand.STOMP -> Stomp(StompConnectHeaders(headers))
            StompCommand.CONNECT -> Connect(StompConnectHeaders(headers))
            StompCommand.CONNECTED -> Connected(StompConnectedHeaders(headers))
            StompCommand.MESSAGE -> Message(StompMessageHeaders(headers), body)
            StompCommand.RECEIPT -> Receipt(StompReceiptHeaders(headers))
            StompCommand.SEND -> Send(StompSendHeaders(headers), body)
            StompCommand.SUBSCRIBE -> Subscribe(StompSubscribeHeaders(headers))
            StompCommand.UNSUBSCRIBE -> Unsubscribe(StompUnsubscribeHeaders(headers))
            StompCommand.ACK -> Ack(StompAckHeaders(headers))
            StompCommand.NACK -> Nack(StompNackHeaders(headers))
            StompCommand.BEGIN -> Begin(StompBeginHeaders(headers))
            StompCommand.COMMIT -> Commit(StompCommitHeaders(headers))
            StompCommand.ABORT -> Abort(StompAbortHeaders(headers))
            StompCommand.DISCONNECT -> Disconnect(StompDisconnectHeaders(headers))
            StompCommand.ERROR -> Error(StompErrorHeaders(headers), body)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
sealed class FrameBody {

    abstract val bytes: ByteArray

    data class Text(
        val text: String
    ) : FrameBody() {
        constructor(bytes: ByteArray) : this(bytes.decodeToString())

        // Text frames must be encoded using UTF-8 as per WebSocket specification
        // If other encodings are needed, the application must use binary frames with relevant content-type header
        override val bytes by lazy { text.encodeToByteArray() }
    }

    data class Binary(
        override val bytes: ByteArray
    ) : FrameBody() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Binary

            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun FrameBody.asText() = when (this) {
    is FrameBody.Binary -> bytes.decodeToString(throwOnInvalidSequence = true)
    is FrameBody.Text -> text
}
