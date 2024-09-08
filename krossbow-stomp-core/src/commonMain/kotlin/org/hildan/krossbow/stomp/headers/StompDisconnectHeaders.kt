package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.frame.StompFrame

/**
 * The headers of a [StompFrame.Disconnect] frame.
 */
interface StompDisconnectHeaders : StompHeaders

/**
 * A temporary mutable representation of [StompDisconnectHeaders] to ease their construction (or copy with modification).
 */
interface StompDisconnectHeadersBuilder : StompDisconnectHeaders, StompHeadersBuilder

/**
 * Creates an instance of [StompDisconnectHeaders].
 * Extra headers can be configured using the [configure] lambda.
 */
fun StompDisconnectHeaders(
    configure: StompDisconnectHeadersBuilder.() -> Unit = {},
): StompDisconnectHeaders = MapBasedStompDisconnectHeaders().apply {
    configure()
}

@Deprecated(
    message = "This overload will be removed in a future version, please use the overload with lambda instead to set optional headers.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
        expression = "StompDisconnectHeaders { this.receipt = receipt }",
        imports = [ "org.hildan.krossbow.stomp.headers.StompDisconnectHeaders" ],
    ),
)
fun StompDisconnectHeaders(
    receipt: String? = null,
): StompDisconnectHeaders = StompDisconnectHeaders { this.receipt = receipt }

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompDisconnectHeaders.copy(transform: StompDisconnectHeadersBuilder.() -> Unit = {}): StompDisconnectHeaders =
    MapBasedStompDisconnectHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompDisconnectHeaders(rawHeaders: MutableMap<String, String>): StompDisconnectHeaders =
    MapBasedStompDisconnectHeaders(backingMap = rawHeaders)

private class MapBasedStompDisconnectHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompDisconnectHeadersBuilder
