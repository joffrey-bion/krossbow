package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.headers.HeaderNames.CONTENT_LENGTH
import org.hildan.krossbow.stomp.headers.HeaderNames.CONTENT_TYPE
import org.hildan.krossbow.stomp.headers.HeaderNames.RECEIPT

/**
 * The headers of a STOMP frame.
 */
sealed interface StompHeaders {
    /**
     * This header is an octet count for the length of the message body.
     *
     * If a `content-length` header is included, this number of octets MUST be read, regardless of whether there
     * are NULL octets in the body. The frame still needs to be terminated with a NULL octet.
     *
     * If the frame body contains NULL octets, the frame MUST include a content-length header.
     */
    val contentLength: Int?

    /**
     * If this header is set, its value MUST be a MIME type that describes the format of the body.
     * Otherwise, the receiver SHOULD consider the body to be a binary blob.
     *
     * The implied text encoding for MIME types starting with `text/` is UTF-8. If you are using a text-based MIME type
     * with a different encoding then you SHOULD append `;charset=<encoding>` to the MIME type.
     *
     * For example, `text/html;charset=utf-16` SHOULD be used if you're sending an HTML body in UTF-16 encoding.
     * The `;charset=<encoding>` SHOULD also get appended to any non `text/` MIME types which can be interpreted as text.
     * A good example of this would be a UTF-8 encoded XML.
     * Its content-type SHOULD get set to `application/xml;charset=utf-8`.
     *
     * All STOMP clients and servers MUST support UTF-8 encoding and decoding. Therefore, for maximum interoperability
     * in a heterogeneous computing environment, it is RECOMMENDED that text-based content be encoded with UTF-8.
     */
    val contentType: String?

    /**
     * Any client frame other than `CONNECT` MAY specify a `receipt` header with an arbitrary value.
     * This will cause the server to acknowledge the processing of the client frame with a RECEIPT frame.
     */
    val receipt: String?

    /**
     * Gets the header with the given [headerName], or null if it's not present.
     * Usually, headers should be accessed via type-safe properties, but this is useful to access custom headers.
     */
    operator fun get(headerName: String): String?

    /**
     * Returns a [Map] view of these headers.
     */
    fun asMap(): Map<String, String>
}

/**
 * A temporary mutable representation of [StompHeaders] to ease their construction (or copy with modification).
 */
sealed interface StompHeadersBuilder : StompHeaders { // override mostly to get docs for free
    override var contentLength: Int?
    override var contentType: String?
    override var receipt: String?

    /**
     * Sets the header named [headerName] to the given [headerValue].
     *
     * Standard headers should be set using type-safe accessors, so this untyped approach should be exclusively used
     * for custom headers.
     *
     * The specification is rather imprecise as to which frames may or may not contain custom headers.
     * The following sentence opens the door for custom headers in all frames:
     *
     * > Finally, STOMP servers MAY use additional headers to give access to features like persistence or expiration.
     * > Consult your server's documentation for details.
     *
     * For this reason, custom headers are allowed in every frame here. Please use this method carefully.
     */
    operator fun set(headerName: String, headerValue: String?)

    /**
     * Sets all the given [headers].
     *
     * Standard headers should usually be set using type-safe accessors, unless setting them in a batch is necessary.
     * Therefore, this untyped approach should mostly be used for custom headers.
     *
     * The specification is rather imprecise as to which frames may or may not contain custom headers.
     * The following sentence opens the door for custom headers in all frames:
     *
     * > Finally, STOMP servers MAY use additional headers to give access to features like persistence or expiration.
     * > Consult your server's documentation for details.
     *
     * For this reason, custom headers are allowed in every frame here. Please use this method carefully.
     */
    fun setAll(headers: Map<String, String>)
}

/**
 * An implementation of [StompHeadersBuilder] backed by a [MutableMap].
 * This can be used as a base for each header type implementation.
 *
 * It allows type-safe access to the headers, but also avoids extra instances and copies (during encoding/decoding) by
 * directly storing the headers in the backing map.
 */
internal abstract class MapBasedStompHeaders(
    internal val backingMap: MutableMap<String, String>,
) : StompHeadersBuilder {
    override var contentLength: Int? by optionalHeader(
        name = CONTENT_LENGTH,
        default = null,
        decode = { it.toIntOrNull() ?: error("invalid 'content-length' header '$it'") },
        encode = { it?.toString() },
    )
    override var contentType: String? by optionalHeader(CONTENT_TYPE)
    override var receipt: String? by optionalHeader(RECEIPT)

    override fun get(headerName: String): String? = backingMap[headerName]

    override fun set(headerName: String, headerValue: String?) {
        if (headerValue == null) {
            backingMap.remove(headerName)
        } else {
            backingMap[headerName] = headerValue
        }
    }

    override fun setAll(headers: Map<String, String>) {
        backingMap.putAll(headers)
    }

    override fun asMap(): Map<String, String> = backingMap

    override fun hashCode(): Int = backingMap.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as MapBasedStompHeaders
        return backingMap == other.backingMap
    }

    override fun toString(): String = "StompHeaders${backingMap}"
}
