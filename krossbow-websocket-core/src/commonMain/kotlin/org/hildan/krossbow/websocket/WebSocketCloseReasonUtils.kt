package org.hildan.krossbow.websocket

import okio.utf8Size

/**
 * The maximum number of UTF-8 bytes allowed by the web socket protocol for the "reason" in close frames.
 * This limit is defined in [section 5.5 of the RFC-6455](https://tools.ietf.org/html/rfc6455#section-5.5).
 */
const val CLOSE_REASON_MAX_LENGTH_BYTES = 123

/**
 * Truncates this string to 123 bytes ([CLOSE_REASON_MAX_LENGTH_BYTES]), for it to be suitable as web socket close
 * reason.
 *
 * The "reason" in WS close frames must not be longer than 123 *bytes* (not characters!) when encoded in UTF-8, due to
 * the limit on control frames defined by the web socket protocol specification
 * [RFC-6455](https://tools.ietf.org/html/rfc6455#section-5.5).
 */
fun String.truncateToCloseFrameReasonLength(): String = truncateUtf8BytesLengthTo(CLOSE_REASON_MAX_LENGTH_BYTES)

/**
 * Returns the biggest prefix of this string that can be represented with at most [maxLength] UTF-8 bytes.
 *
 * If the truncation occurs in the middle of a multibyte-character, the whole character is removed.
 */
fun String.truncateUtf8BytesLengthTo(maxLength: Int): String {
    if (utf8Size() <= maxLength) {
        return this
    }
    return encodeToByteArray().copyOf(maxLength).decodeToString().trimEnd { it == '\uFFFD' }
}
