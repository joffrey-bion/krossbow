package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.MESSAGE
import org.hildan.krossbow.stomp.headers.HeaderNames.RECEIPT_ID

/**
 * The headers of a [StompFrame.Error] frame.
 */
interface StompErrorHeaders : StompHeaders {
    /**
     * An optional message describing the error.
     */
    val message: String?
    /**
     * The receipt ID of the frame that caused this error, or null if it didn't have a `receipt` header.
     */
    val receiptId: String?
}

/**
 * A temporary mutable representation of [StompErrorHeaders] to ease their construction (or copy with modification).
 */
interface StompErrorHeadersBuilder : StompErrorHeaders, StompHeadersBuilder {
    override var message: String?
    override var receiptId: String?
}

/**
 * Creates an instance of [StompErrorHeaders].
 * All headers are optional and can be configured using the [configure] lambda.
 */
fun StompErrorHeaders(configure: StompErrorHeadersBuilder.() -> Unit = {}): StompErrorHeaders =
    MapBasedStompErrorHeaders().apply(configure)

@Deprecated(
    message = "This overload will be removed in a future version, please use the overload with lambda instead to set optional headers.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
        expression = "StompErrorHeaders {\n" +
            "    this.message = message\n" +
            "    this.receiptId = receiptId\n" +
            "    this.putAll(customHeaders)\n" +
            "}",
        imports = [ "org.hildan.krossbow.stomp.headers.StompErrorHeaders" ],
    ),
)
fun StompErrorHeaders(
    message: String? = null,
    receiptId: String? = null,
    customHeaders: Map<String, String> = emptyMap(),
): StompErrorHeaders = StompErrorHeaders {
    this.message = message
    this.receiptId = receiptId
    this.setAll(customHeaders)
}

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompErrorHeaders.copy(transform: StompErrorHeadersBuilder.() -> Unit = {}): StompErrorHeaders =
    MapBasedStompErrorHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompErrorHeaders(rawHeaders: MutableMap<String, String>): StompErrorHeaders =
    MapBasedStompErrorHeaders(backingMap = rawHeaders)

private class MapBasedStompErrorHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompErrorHeadersBuilder {
    override var message: String? by optionalHeader(MESSAGE)
    override var receiptId: String? by optionalHeader(RECEIPT_ID)
}
