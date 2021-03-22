package org.hildan.krossbow.stomp.frame

import org.hildan.krossbow.stomp.charsets.Charset
import org.hildan.krossbow.stomp.charsets.decode

/**
 * Represents the body of a [StompFrame].
 *
 * If a STOMP frame was received as a binary web socket frame, its body will be a [Binary] body.
 * If a STOMP frame was received as a textual web socket frame, its body will be a [Text] body.
 */
@OptIn(ExperimentalStdlibApi::class)
sealed class FrameBody {

    /**
     * The bytes of this frame body. For a text frame, the text is encoded as UTF-8.
     */
    abstract val bytes: ByteArray

    /**
     * Represents the body of a [StompFrame] that was received as a textual web socket frame.
     */
    data class Text(val text: String) : FrameBody() {
        constructor(utf8Bytes: ByteArray) : this(utf8Bytes.decodeToString())

        // Text frames must be encoded using UTF-8 as per WebSocket specification
        // If other encodings are needed, the application must use binary frames with relevant content-type header
        override val bytes by lazy { text.encodeToByteArray() }
    }

    /**
     * Represents the body of a [StompFrame] that was received as a binary web socket frame.
     */
    data class Binary(override val bytes: ByteArray) : FrameBody() {

        internal fun decodeAsText(charset: Charset): String = charset.newDecoder().decode(bytes)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Binary
            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }
}
