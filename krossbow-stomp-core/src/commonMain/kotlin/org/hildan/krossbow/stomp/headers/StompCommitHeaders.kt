package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.TRANSACTION

/**
 * The headers of a [StompFrame.Commit] frame.
 */
interface StompCommitHeaders : StompHeaders {
    /**
     * The ID of the transaction to commit.
     */
    val transaction: String
}

/**
 * A temporary mutable representation of [StompCommitHeaders] to ease their construction (or copy with modification).
 */
interface StompCommitHeadersBuilder : StompCommitHeaders, StompHeadersBuilder {
    override var transaction: String
}

/**
 * Creates an instance of [StompCommitHeaders] with the given [transaction] header.
 * Extra headers can be configured using the [configure] lambda.
 */
fun StompCommitHeaders(
    transaction: String,
    configure: StompCommitHeadersBuilder.() -> Unit = {},
): StompCommitHeaders = MapBasedStompCommitHeaders().apply {
    this.transaction = transaction
    configure()
}

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompCommitHeaders.copy(transform: StompCommitHeadersBuilder.() -> Unit = {}): StompCommitHeaders =
    MapBasedStompCommitHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompCommitHeaders(rawHeaders: MutableMap<String, String>): StompCommitHeaders =
    MapBasedStompCommitHeaders(backingMap = rawHeaders)

private class MapBasedStompCommitHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompCommitHeadersBuilder {
    override var transaction: String by requiredHeader(TRANSACTION)
}
