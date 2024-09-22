package org.hildan.krossbow.stomp.frame

import org.hildan.krossbow.stomp.charsets.Charset
import org.hildan.krossbow.stomp.charsets.extractCharset
import org.hildan.krossbow.stomp.headers.*

/**
 * This is an internal parent class to gather [StompFrame]s and other internal events under the same common type.
 *
 * [StompFrame] directly extend it to avoid the overhead of wrapping every frame in a [StompEvent] internally.
 */
sealed interface StompEvent

/**
 * Represents data-less traffic that needs to be registered to reset heartbeat counters.
 */
internal data object StompHeartBeat : StompEvent

/**
 * Materializes completion of the STOMP frames flow.
 */
internal data object StompConnectionClosed : StompEvent

/**
 * Materializes errors in the STOMP frames flow.
 */
internal data class StompConnectionError(val cause: Throwable) : StompEvent

/**
 * Represents a STOMP frame. The structure of STOMP frames is
 * [defined by the specification](https://stomp.github.io/stomp-specification-1.2.html#STOMP_Frames).
 */
sealed interface StompFrame : StompEvent {
    /** The command of this STOMP frame, which is the first word of the frame. */
    @Deprecated(
        message = "This property is an implementation detail of frame encoding and will be removed in a future " +
            "release. Please use 'is' checks on the frame type if you need to distinguish particular frames.",
    )
    val command: StompCommand
    /** The headers of this STOMP frame. */
    val headers: StompHeaders
    /** The body of this STOMP frame. */
    val body: FrameBody?

    /**
     * The body of this frame as text.
     *
     * If this STOMP frame comes from a text web socket frame, then [bodyAsText] is simply the body's text.
     *
     * If this STOMP frame comes from a binary web socket frame, the binary body is decoded to text based on the
     * `content-type` header of this frame.
     * The `content-type` header must be present, and its value must start with `text/` or contain an explicit charset,
     * as defined in the [specification](https://stomp.github.io/stomp-specification-1.2.html#Header_content-type).
     * If there is no `content-type` header, or if the charset cannot be properly extracted/inferred from it, accessing
     * [bodyAsText] throws an exception.
     *
     * The STOMP protocol doesn't distinguish between missing bodies and 0-length bodies.
     * For this reason and for convenience, [bodyAsText] is the empty string in both cases.
     */
    val bodyAsText: String
        get() = body?.asText(headers.contentType) ?: ""

    sealed interface ControlFrame : StompFrame {
        override val body: Nothing? get() = null
    }

    sealed interface ClientFrame : StompFrame

    sealed interface ServerFrame : StompFrame

    /**
     * A STOMP frame is a STOMP 1.2 replacement for the CONNECT frame, used to start a STOMP session on a web socket
     * connection.
     *
     * Clients that use the STOMP frame instead of the CONNECT frame will only be able to connect to STOMP 1.2 servers
     * (as well as some STOMP 1.1 servers), but the advantage is that a protocol sniffer/discriminator will be able to
     * differentiate the STOMP connection from an HTTP connection.
     */
    data class Stomp(override val headers: StompConnectHeaders) : ClientFrame, ControlFrame {
        override val command: StompCommand get() = StompCommand.STOMP
    }

    /**
     * A CONNECT frame is a client frame used to start a STOMP session on a web socket connection.
     */
    data class Connect(override val headers: StompConnectHeaders) : ClientFrame, ControlFrame {
        override val command: StompCommand get() = StompCommand.CONNECT
    }

    /**
     * A CONNECTED frame is a server frame received upon successful connection at the STOMP protocol level.
     */
    data class Connected(override val headers: StompConnectedHeaders) : ServerFrame, ControlFrame {
        override val command: StompCommand get() = StompCommand.CONNECTED
    }

    /**
     * A SEND frame is a client frame used to send a message to a destination in the messaging system.
     * The optional body of the SEND frame is the message to be sent.
     */
    data class Send(
        override val headers: StompSendHeaders,
        override val body: FrameBody?,
    ) : ClientFrame, StompFrame {
        override val command: StompCommand get() = StompCommand.SEND
    }

    /**
     * A SUBSCRIBE frame is a client frame used to register to listen to a given destination.
     *
     * Any messages received on the subscribed destination will henceforth be delivered as MESSAGE frames from the
     * server to the client. The `ack` header controls the message acknowledgment mode.
     */
    data class Subscribe(override val headers: StompSubscribeHeaders) : ClientFrame, ControlFrame {
        override val command: StompCommand get() = StompCommand.SUBSCRIBE
    }

    /**
     * An UNSUBSCRIBE frame is a client frame used to stop a STOMP subscription.
     */
    data class Unsubscribe(override val headers: StompUnsubscribeHeaders) : ClientFrame, ControlFrame {
        override val command: StompCommand get() = StompCommand.UNSUBSCRIBE
    }

    /**
     * A MESSAGE frame is a server frame used to convey a message from a subscription to the client.
     * A MESSAGE frame must be part of a subscription.
     */
    data class Message(
        override val headers: StompMessageHeaders,
        override val body: FrameBody?,
    ) : ServerFrame {
        override val command: StompCommand get() = StompCommand.MESSAGE
    }

    /**
     * A RECEIPT frame is sent from the server to the client once the server has successfully processed a client frame
     * that requests a receipt.
     * It is expected from the server when the client frame has a `receipt` header.
     */
    data class Receipt(override val headers: StompReceiptHeaders) : ServerFrame, ControlFrame {
        override val command: StompCommand get() = StompCommand.RECEIPT
    }

    /**
     * An ACK frame is a client frame used to acknowledge consumption of a message from a subscription using `client` or
     * `client-individual` acknowledgment.
     * Any messages received from such a subscription will not be considered to have been consumed until the message has
     * been acknowledged via an ACK.
     */
    data class Ack(override val headers: StompAckHeaders) : ClientFrame, ControlFrame {
        override val command: StompCommand get() = StompCommand.ACK
    }

    /**
     * A NACK frame is a client frame used to tell the server that the client did not consume the message.
     * The server can then either send the message to a different client, discard it, or put it in a dead letter queue.
     */
    data class Nack(override val headers: StompNackHeaders) : ClientFrame, ControlFrame {
        override val command: StompCommand get() = StompCommand.NACK
    }

    /**
     * A BEGIN frame is a client frame used to start a transaction.
     *
     * Transactions in this case apply to sending and acknowledging - any messages sent or acknowledged during a
     * transaction will be processed atomically based on the transaction.
     */
    data class Begin(override val headers: StompBeginHeaders) : ClientFrame, ControlFrame {
        override val command: StompCommand get() = StompCommand.BEGIN
    }

    /**
     * A COMMIT frame is a client frame used to commit a transaction in progress.
     */
    data class Commit(override val headers: StompCommitHeaders) : ClientFrame, ControlFrame {
        override val command: StompCommand get() = StompCommand.COMMIT
    }

    /**
     * An ABORT frame is a client frame used to roll back a transaction in progress.
     */
    data class Abort(override val headers: StompAbortHeaders) : ClientFrame, ControlFrame {
        override val command: StompCommand get() = StompCommand.ABORT
    }

    /**
     * A DISCONNECT frame is a client frame used to gracefully disconnect from the server.
     * Waiting for a RECEIPT on DISCONNECT ensures that all previously sent messages have been received by the server.
     */
    data class Disconnect(override val headers: StompDisconnectHeaders) : ClientFrame, ControlFrame {
        override val command: StompCommand get() = StompCommand.DISCONNECT
    }

    /**
     * An ERROR frame is a server frame sent in case of error.
     * Receiving an ERROR frame implies that the connection should be closed and no further messages can be sent or
     * received.
     */
    data class Error(
        override val headers: StompErrorHeaders,
        override val body: FrameBody?,
    ) : ServerFrame {
        override val command: StompCommand get() = StompCommand.ERROR
        /**
         * The description of the error, taken from the `message` header if present, or from the body.
         */
        internal val message: String = headers.message ?: (body as? FrameBody.Text)?.text ?: "(binary error message)"
    }

    companion object // kept for backwards compatibility
}

private fun FrameBody.asText(contentType: String?): String = when (this) {
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
    // charset=utf-16 SHOULD be used if you're sending an HTML body in UTF-16 encoding. The ;charset= SHOULD also get
    // appended to any non text/ MIME types which can be interpreted as text. A good example of this would be a UTF-8
    // encoded XML. Its content-type SHOULD get set to application/xml;charset=utf-8"
    val charset = extractCharset(contentTypeHeader)
    return when {
        charset != null -> charset
        contentTypeHeader.startsWith("text/") -> Charset.UTF_8
        else -> throw UnsupportedOperationException(
            "Binary frame with content-type '$contentTypeHeader' cannot be converted to text"
        )
    }
}

/**
 * Creates a copy of this [StompFrame] with the headers changed via [updateHeadersCopy].
 * This operation doesn't mutate the headers of this [StompFrame].
 */
// Note: this is a poor man's copy function, but adding it to the interface would require making the interface generic,
// and would mean more bloat in the headers classes (which should be redesigned soon anyway).
@Suppress("UNCHECKED_CAST")
internal fun <T : StompFrame> T.copyWithHeaders(updateHeadersCopy: StompHeadersBuilder.() -> Unit): T = when (this) {
    is StompFrame.Connect -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Connected -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Send -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Subscribe -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Unsubscribe -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Disconnect -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Ack -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Nack -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Begin -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Commit -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Abort -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Message -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Receipt -> copy(headers.copy(updateHeadersCopy)) as T
    is StompFrame.Error -> copy(headers.copy(updateHeadersCopy)) as T
    else -> error("Unknown StompFrame type ${this::class.simpleName}") // qualifiedName cannot be used in JS
}
