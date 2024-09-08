package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.DESTINATION
import org.hildan.krossbow.stomp.headers.HeaderNames.TRANSACTION

/**
 * The headers of a [StompFrame.Send] frame.
 */
sealed interface StompSendHeaders : StompHeaders {
    /**
     * The destination to send this message to.
     */
    val destination: String
    /**
     * The ID of the transaction that this message is part of.
     */
    val transaction: String?
}

/**
 * A temporary mutable representation of [StompSendHeaders] to ease their construction (or copy with modification).
 */
sealed interface StompSendHeadersBuilder : StompSendHeaders, StompHeadersBuilder {
    override var destination: String
    override var transaction: String?
}

/**
 * Creates an instance of [StompSendHeaders] with the given [destination].
 * Optional headers can be configured using the [configure] lambda.
 */
fun StompSendHeaders(
    destination: String,
    configure: StompSendHeadersBuilder.() -> Unit = {},
): StompSendHeaders = MapBasedStompSendHeaders().apply {
    this.destination = destination
    configure()
}

@Deprecated(
    message = "This overload will be removed in a future version, please use the overload with lambda instead to set optional headers.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
        expression = "StompSendHeaders(destination) {\n" +
            "    this.transaction = transaction\n" +
            "    this.receipt = receipt\n" +
            "    putAll(customHeaders)\n" +
            "}",
        imports = [ "org.hildan.krossbow.stomp.headers.StompSendHeaders" ],
    ),
)
fun StompSendHeaders(
    destination: String,
    transaction: String? = null,
    receipt: String? = null,
    customHeaders: Map<String, String> = emptyMap(),
) = StompSendHeaders(destination) {
    this.transaction = transaction
    this.receipt = receipt
    setAll(customHeaders)
}

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompSendHeaders.copy(transform: StompSendHeadersBuilder.() -> Unit = {}): StompSendHeaders =
    MapBasedStompSendHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompSendHeaders(rawHeaders: MutableMap<String, String>): StompSendHeaders =
    MapBasedStompSendHeaders(backingMap = rawHeaders)

private class MapBasedStompSendHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompSendHeadersBuilder {
    override var destination: String by requiredHeader(DESTINATION)
    override var transaction: String? by optionalHeader(TRANSACTION)
}
