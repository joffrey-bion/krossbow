package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.TRANSACTION

/**
 * The headers of a [StompFrame.Begin] frame.
 */
interface StompBeginHeaders : StompHeaders {
    /**
     * The ID to define for the new transaction that we begin.
     */
    val transaction: String
}

/**
 * A temporary mutable representation of [StompBeginHeaders] to ease their construction (or copy with modification).
 */
interface StompBeginHeadersBuilder : StompBeginHeaders, StompHeadersBuilder {
    override var transaction: String
}

/**
 * Creates an instance of [StompBeginHeaders] with the given [transaction] header.
 * Extra headers can be configured using the [configure] lambda.
 */
fun StompBeginHeaders(
    transaction: String,
    configure: StompBeginHeadersBuilder.() -> Unit = {},
): StompBeginHeaders = MapBasedStompBeginHeaders().apply {
    this.transaction = transaction
    configure()
}

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompBeginHeaders.copy(transform: StompBeginHeadersBuilder.() -> Unit = {}): StompBeginHeaders =
    MapBasedStompBeginHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompBeginHeaders(rawHeaders: MutableMap<String, String>): StompBeginHeaders =
    MapBasedStompBeginHeaders(backingMap = rawHeaders)

private class MapBasedStompBeginHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompBeginHeadersBuilder {
    override var transaction: String by requiredHeader(TRANSACTION)
}
