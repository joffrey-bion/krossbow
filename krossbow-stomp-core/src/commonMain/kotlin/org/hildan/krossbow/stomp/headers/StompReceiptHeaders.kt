package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.RECEIPT_ID

/**
 * The headers of a [StompFrame.Receipt] frame.
 */
sealed interface StompReceiptHeaders : StompHeaders {
    /**
     * The value of the `receipt` header of the frame that this is a receipt for.
     */
    val receiptId: String
}

/**
 * A temporary mutable representation of [StompReceiptHeaders] to ease their construction (or copy with modification).
 */
sealed interface StompReceiptHeadersBuilder : StompReceiptHeaders, StompHeadersBuilder {
    override var receiptId: String
}

/**
 * Creates an instance of [StompReceiptHeaders] with the given [receiptId] header.
 * Extra headers can be configured using the [configure] lambda.
 */
fun StompReceiptHeaders(
    receiptId: String,
    configure: StompReceiptHeadersBuilder.() -> Unit = {},
): StompReceiptHeaders = MapBasedStompReceiptHeaders().apply {
    this.receiptId = receiptId
    configure()
}

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompReceiptHeaders.copy(transform: StompReceiptHeadersBuilder.() -> Unit = {}): StompReceiptHeaders =
    MapBasedStompReceiptHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompReceiptHeaders(rawHeaders: MutableMap<String, String>): StompReceiptHeaders =
    MapBasedStompReceiptHeaders(backingMap = rawHeaders)

private class MapBasedStompReceiptHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompReceiptHeadersBuilder {
    override var receiptId: String by requiredHeader(RECEIPT_ID)
}