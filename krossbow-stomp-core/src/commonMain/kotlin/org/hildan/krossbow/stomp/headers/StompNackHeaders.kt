package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.ID
import org.hildan.krossbow.stomp.headers.HeaderNames.TRANSACTION

/**
 * The headers of a [StompFrame.Nack] frame.
 */
interface StompNackHeaders : StompHeaders {
    /**
     * The value of the `ack` header of the message to refuse.
     */
    val id: String
    /**
     * The ID of the transaction that this message is part of.
     */
    val transaction: String?
}

/**
 * A temporary mutable representation of [StompNackHeaders] to ease their construction (or copy with modification).
 */
interface StompNackHeadersBuilder : StompNackHeaders, StompHeadersBuilder {
    override var id: String
    override var transaction: String?
}

/**
 * Creates an instance of [StompNackHeaders] with the given [id] header.
 * Optional headers can be configured using the [configure] lambda.
 */
fun StompNackHeaders(id: String, configure: StompNackHeadersBuilder.() -> Unit = {}): StompNackHeaders =
    MapBasedStompNackHeaders().apply {
        this.id = id
        configure()
    }

@Deprecated(
    message = "This overload will be removed in a future version, please use the overload with lambda instead to set optional headers.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
        expression = "StompNackHeaders(id) { this.transaction = transaction }",
        imports = [ "org.hildan.krossbow.stomp.headers.StompNackHeaders" ],
    ),
)
fun StompNackHeaders(
    id: String,
    transaction: String? = null,
): StompNackHeaders = StompNackHeaders(id) { this.transaction = transaction }

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompNackHeaders.copy(transform: StompNackHeadersBuilder.() -> Unit = {}): StompNackHeaders =
    MapBasedStompNackHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompNackHeaders(rawHeaders: MutableMap<String, String>): StompNackHeaders =
    MapBasedStompNackHeaders(backingMap = rawHeaders)

private class MapBasedStompNackHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompNackHeadersBuilder {
    override var id: String by requiredHeader(ID)
    override var transaction: String? by optionalHeader(TRANSACTION)
}