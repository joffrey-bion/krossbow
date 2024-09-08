package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.TRANSACTION

/**
 * The headers of a [StompFrame.Abort] frame.
 */
interface StompAbortHeaders : StompHeaders {
    /**
     * The ID of the transaction to abort.
     */
    val transaction: String
}

/**
 * A temporary mutable representation of [StompAbortHeaders] to ease their construction (or copy with modification).
 */
interface StompAbortHeadersBuilder : StompAbortHeaders, StompHeadersBuilder {
    override var transaction: String
}

/**
 * Creates an instance of [StompAbortHeaders] with the given [transaction] header.
 * Extra headers can be configured using the [configure] lambda.
 */
fun StompAbortHeaders(
    transaction: String,
    configure: StompAbortHeadersBuilder.() -> Unit = {},
): StompAbortHeaders = MapBasedStompAbortHeaders().apply {
    this.transaction = transaction
    configure()
}

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompAbortHeaders.copy(transform: StompAbortHeadersBuilder.() -> Unit = {}): StompAbortHeaders =
    MapBasedStompAbortHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompAbortHeaders(rawHeaders: MutableMap<String, String>): StompAbortHeaders =
    MapBasedStompAbortHeaders(backingMap = rawHeaders)

private class MapBasedStompAbortHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompAbortHeadersBuilder {
    override var transaction: String by requiredHeader(TRANSACTION)
}
