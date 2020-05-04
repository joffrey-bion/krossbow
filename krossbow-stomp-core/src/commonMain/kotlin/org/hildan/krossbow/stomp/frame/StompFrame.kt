package org.hildan.krossbow.stomp.frame

import kotlinx.io.charsets.Charset
import kotlinx.io.charsets.Charsets
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
import org.hildan.krossbow.utils.extractCharset

/**
 * Represents a STOMP frame. The structure of STOMP frames is
 * [defined by the specification](https://stomp.github.io/stomp-specification-1.2.html#STOMP_Frames).
 */
sealed class StompFrame(
    /** The command of this STOMP frame, which is the first word of the frame. */
    val command: StompCommand,
    /** The headers of this STOMP frame. */
    open val headers: StompHeaders,
    /** The body of this STOMP frame. */
    open val body: FrameBody? = null
) {
    /**
     * The body of this frame as text.
     *
     * If this STOMP frame comes from a binary web socket frame, the binary body is converted to text based on the
     * `content-type` header of this frame. This conversion respects the conventions defined by the
     * [STOMP specification](https://stomp.github.io/stomp-specification-1.2.html#Header_content-type).
     *
     * If this STOMP frame comes from a text web socket frame, then [bodyAsText] is simply the body's text.
     *
     * The STOMP protocol doesn't distinguish between absent frame bodies and 0-length bodies.
     * For this reason and for convenience, [bodyAsText] is the empty string in this case.
     */
    val bodyAsText: String by lazy { body?.asText(headers.contentType) ?: "" }

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

        internal fun create(
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

private fun FrameBody.asText(contentType: String?): String? = when (this) {
    is FrameBody.Text -> text
    is FrameBody.Binary -> decodeAsText(inferCharset(contentType))
}

// From the specification: https://stomp.github.io/stomp-specification-1.2.html#Header_content-type
private fun inferCharset(contentTypeHeader: String?): Charset {
    if (contentTypeHeader == null) {
        // "If the content-type header is set, its value MUST be a MIME type which describes the format of the body.
        // Otherwise, the receiver SHOULD consider the body to be a binary blob."
        throw UnsupportedOperationException("Binary frame without content-type header cannot be converted to text")
    }
    // "The implied text encoding for MIME types starting with text/ is UTF-8. If you are using a text based MIME type
    // with a different encoding then you SHOULD append ;charset= to the MIME type. For example, text/html;
    // charset=utf-16 SHOULD be used if your sending an HTML body in UTF-16 encoding. The ;charset= SHOULD also get
    // appended to any non text/ MIME types which can be interpreted as text. A good example of this would be a UTF-8
    // encoded XML. Its content-type SHOULD get set to application/xml;charset=utf-8"
    val charset = extractCharset(contentTypeHeader)
    return when {
        charset != null -> charset
        contentTypeHeader.startsWith("text/") -> Charsets.UTF_8
        else -> throw UnsupportedOperationException("Binary frame with content-type '$contentTypeHeader' cannot be " +
                "converted to text")
    }
}
