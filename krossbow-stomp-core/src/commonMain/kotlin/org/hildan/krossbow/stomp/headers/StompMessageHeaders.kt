package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.ACK
import org.hildan.krossbow.stomp.headers.HeaderNames.DESTINATION
import org.hildan.krossbow.stomp.headers.HeaderNames.MESSAGE_ID
import org.hildan.krossbow.stomp.headers.HeaderNames.SUBSCRIPTION

/**
 * The headers of a [StompFrame.Message] frame.
 */
interface StompMessageHeaders : StompHeaders {
    /**
     * The destination this message was received from.
     */
    val destination: String
    /**
     * The ID of this message.
     */
    val messageId: String
    /**
     * The subscription this message is associated with.
     */
    val subscription: String
    /**
     * The value to use for a corresponding ACK or NACK frame.
     *
     * If the message is received from a subscription that requires explicit acknowledgment (either [AckMode.CLIENT] or
     * [AckMode.CLIENT_INDIVIDUAL]), then the MESSAGE frame MUST also contain an ack header with an arbitrary value.
     * This header will be used to relate the message to a subsequent ACK or NACK frame.
     */
    val ack: String?
}

/**
 * A temporary mutable representation of [StompMessageHeaders] to ease their construction (or copy with modification).
 */
interface StompMessageHeadersBuilder : StompMessageHeaders, StompHeadersBuilder {
    override var destination: String
    override var messageId: String
    override var subscription: String
    override var ack: String?
}

/**
 * Creates an instance of [StompMessageHeaders] with the given [destination], [messageId], and [subscription].
 * Optional headers can be configured using the [configure] lambda.
 */
fun StompMessageHeaders(
    destination: String,
    messageId: String,
    subscription: String,
    configure: StompMessageHeadersBuilder.() -> Unit = {},
): StompMessageHeaders = MapBasedStompMessageHeaders().apply {
    this.destination = destination
    this.messageId = messageId
    this.subscription = subscription
    configure()
}

@Deprecated(
    message = "This overload will be removed in a future version, please use the overload with lambda instead to set optional headers.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
        expression = "StompMessageHeaders(destination, messageId, subscription) {\n" +
            "    this.ack = ack\n" +
            "    putAll(customHeaders)\n" +
            "}",
        imports = [ "org.hildan.krossbow.stomp.headers.StompMessageHeaders" ],
    ),
)
fun StompMessageHeaders(
    destination: String,
    messageId: String,
    subscription: String,
    ack: String? = null,
    customHeaders: Map<String, String> = emptyMap(),
): StompMessageHeaders = StompMessageHeaders(destination, messageId, subscription) {
    this.ack = ack
    setAll(customHeaders)
}

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompMessageHeaders.copy(transform: StompMessageHeadersBuilder.() -> Unit = {}): StompMessageHeaders =
    MapBasedStompMessageHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompMessageHeaders(rawHeaders: MutableMap<String, String>): StompMessageHeaders =
    MapBasedStompMessageHeaders(backingMap = rawHeaders)

private class MapBasedStompMessageHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompMessageHeadersBuilder {
    override var destination: String by requiredHeader(DESTINATION)
    override var messageId: String by requiredHeader(MESSAGE_ID)
    override var subscription: String by requiredHeader(SUBSCRIPTION)
    override var ack: String? by optionalHeader(ACK)
}
