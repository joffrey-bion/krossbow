package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.ID

/**
 * The headers of a [StompFrame.Unsubscribe] frame.
 */
sealed interface StompUnsubscribeHeaders : StompHeaders {
    /**
     * The ID of the subscription to unsubscribe from.
     */
    val id: String
}

/**
 * A temporary mutable representation of [StompUnsubscribeHeaders] to ease their construction (or copy with modification).
 */
sealed interface StompUnsubscribeHeadersBuilder : StompUnsubscribeHeaders, StompHeadersBuilder {
    override var id: String
}

/**
 * Creates an instance of [StompUnsubscribeHeaders] with the given [id] header.
 * Extra headers can be configured using the [configure] lambda.
 */
fun StompUnsubscribeHeaders(
    id: String,
    configure: StompUnsubscribeHeadersBuilder.() -> Unit = {},
): StompUnsubscribeHeaders = MapBasedStompUnsubscribeHeaders().apply {
    this.id = id
    configure()
}

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompUnsubscribeHeaders.copy(transform: StompUnsubscribeHeadersBuilder.() -> Unit = {}): StompUnsubscribeHeaders =
    MapBasedStompUnsubscribeHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompUnsubscribeHeaders(rawHeaders: MutableMap<String, String>): StompUnsubscribeHeaders =
    MapBasedStompUnsubscribeHeaders(backingMap = rawHeaders)

private class MapBasedStompUnsubscribeHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompUnsubscribeHeadersBuilder {
    override var id: String by requiredHeader(ID)
}