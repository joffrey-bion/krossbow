package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.ACK
import org.hildan.krossbow.stomp.headers.HeaderNames.DESTINATION
import org.hildan.krossbow.stomp.headers.HeaderNames.ID
import org.hildan.krossbow.stomp.utils.*

/**
 * The headers of a [StompFrame.Subscribe] frame.
 */
sealed interface StompSubscribeHeaders : StompHeaders {
    /**
     * The destination to subscribe to.
     */
    val destination: String
    /**
     * The ID to define for this new subscription.
     */
    val id: String
    /**
     * The acknowledgment mode for messages in this subscription. Defaults to [AckMode.AUTO].
     */
    val ack: AckMode
}

/**
 * A temporary mutable representation of [StompSubscribeHeaders] to ease their construction (or copy with modification).
 */
sealed interface StompSubscribeHeadersBuilder : StompSubscribeHeaders, StompHeadersBuilder {
    override var destination: String
    override var id: String
    override var ack: AckMode
}

/**
 * Creates an instance of [StompSubscribeHeaders] with the given [destination] header.
 * Optional headers can be configured using the [configure] lambda.
 */
fun StompSubscribeHeaders(
    destination: String,
    configure: StompSubscribeHeadersBuilder.() -> Unit = {},
): StompSubscribeHeaders = MapBasedStompSubscribeHeaders().apply {
    this.destination = destination
    configure()
}

@Deprecated(
    message = "This overload will be removed in a future version, please use the overload with lambda instead to set optional headers.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
        expression = "StompSubscribeHeaders(destination) {\n" +
            "    id?.let { this.id = it }\n" +
            "    this.ack = ack\n" +
            "    this.receipt = receipt\n" +
            "    putAll(customHeaders)\n" +
            "}",
        imports = [ "org.hildan.krossbow.stomp.headers.StompSubscribeHeaders" ],
    ),
)
fun StompSubscribeHeaders(
    destination: String,
    id: String? = null,
    ack: AckMode = AckMode.AUTO,
    receipt: String? = null,
    customHeaders: Map<String, String> = emptyMap(),
) = StompSubscribeHeaders(destination) {
    id?.let { this.id = it }
    this.ack = ack
    this.receipt = receipt
    setAll(customHeaders)
}

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompSubscribeHeaders.copy(transform: StompSubscribeHeadersBuilder.() -> Unit = {}): StompSubscribeHeaders =
    MapBasedStompSubscribeHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompSubscribeHeaders(rawHeaders: MutableMap<String, String>): StompSubscribeHeaders =
    MapBasedStompSubscribeHeaders(backingMap = rawHeaders)

private class MapBasedStompSubscribeHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompSubscribeHeadersBuilder {
    override var destination: String by requiredHeader(DESTINATION)
    override var id: String by requiredHeader(ID, preset = generateUuid())
    override var ack: AckMode by optionalHeader(
        name = ACK,
        default = AckMode.AUTO,
        decode = { AckMode.fromHeader(it) },
        encode = { it.headerValue },
    )
}

/**
 * Represents possible values for the
 * [`ack` header](https://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE_ack_Header).
 */
enum class AckMode(val headerValue: String) {
    /**
     * When the ack mode is auto, then the client does not need to send the server ACK frames for the messages it
     * receives. The server will assume the client has received the message as soon as it sends it to the client. This
     * acknowledgment mode can cause messages being transmitted to the client to get dropped.
     */
    AUTO("auto"),
    /**
     * When the ack mode is client, then the client MUST send the server ACK frames for the messages it processes. If
     * the connection fails before a client sends an ACK frame for the message the server will assume the message has
     * not been processed and MAY redeliver the message to another client. The ACK frames sent by the client will be
     * treated as a cumulative acknowledgment. This means the acknowledgment operates on the message specified in the
     * ACK frame and all messages sent to the subscription before the ACK'ed message.
     */
    CLIENT("client"),
    /**
     * When the ack mode is client-individual, the acknowledgment operates just like the client acknowledgment mode
     * except that the ACK or NACK frames sent by the client are not cumulative. This means that an ACK or NACK frame
     * for a subsequent message MUST NOT cause a previous message to get acknowledged.
     */
    CLIENT_INDIVIDUAL("client-individual");

    companion object {
        private val valuesByText = entries.associateBy { it.headerValue }

        fun fromHeader(value: String) = valuesByText[value] ?: throw InvalidAckHeaderException(value)
    }
}

/**
 * An exception thrown when an unknown value of [AckMode] is found in an `ack` header.
 */
class InvalidAckHeaderException(val invalidText: String) : Exception("Unknown ack header value '$invalidText'")
