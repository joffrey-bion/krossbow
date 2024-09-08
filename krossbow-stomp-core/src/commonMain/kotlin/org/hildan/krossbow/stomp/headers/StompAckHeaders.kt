package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.ID
import org.hildan.krossbow.stomp.headers.HeaderNames.TRANSACTION

/**
 * The headers of a [StompFrame.Ack] frame.
 */
interface StompAckHeaders : StompHeaders {
    /**
     * The value of the `ack` header of the message to acknowledge.
     */
    val id: String
    /**
     * The ID of the transaction that this message is part of.
     */
    val transaction: String?
}

/**
 * A temporary mutable representation of [StompAckHeaders] to ease their construction (or copy with modification).
 */
interface StompAckHeadersBuilder : StompAckHeaders, StompHeadersBuilder {
    override var id: String
    override var transaction: String?
}

/**
 * Creates an instance of [StompAckHeaders] with the given [id] header.
 * Optional headers can be configured using the [configure] lambda.
 */
fun StompAckHeaders(id: String, configure: StompAckHeadersBuilder.() -> Unit = {}): StompAckHeaders =
    MapBasedStompAckHeaders().apply {
        this.id = id
        configure()
    }

@Deprecated(
    message = "This overload will be removed in a future version, please use the overload with lambda instead to set optional headers.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
        expression = "StompAckHeaders(id) { this.transaction = transaction }",
        imports = [ "org.hildan.krossbow.stomp.headers.StompAckHeaders" ],
    ),
)
fun StompAckHeaders(
    id: String,
    transaction: String? = null,
): StompAckHeaders = StompAckHeaders(id) { this.transaction = transaction }

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompAckHeaders.copy(transform: StompAckHeadersBuilder.() -> Unit = {}): StompAckHeaders =
    MapBasedStompAckHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompAckHeaders(rawHeaders: MutableMap<String, String>): StompAckHeaders =
    MapBasedStompAckHeaders(backingMap = rawHeaders)

private class MapBasedStompAckHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompAckHeadersBuilder {
    override var id: String by requiredHeader(ID)
    override var transaction: String? by optionalHeader(TRANSACTION)
}
