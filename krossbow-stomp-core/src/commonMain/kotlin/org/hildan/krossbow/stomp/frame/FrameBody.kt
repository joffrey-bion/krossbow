package org.hildan.krossbow.stomp.frame

import kotlinx.io.bytestring.*
import org.hildan.krossbow.stomp.charsets.Charset
import org.hildan.krossbow.stomp.charsets.decodeToString

/**
 * Represents the body of a [StompFrame].
 *
 * If a STOMP frame was received as a binary web socket frame, its body will be a [Binary] body.
 * If a STOMP frame was received as a textual web socket frame, its body will be a [Text] body.
 */
sealed class FrameBody {

    /**
     * The bytes of this frame body. For a text frame, the text is encoded as UTF-8.
     */
    abstract val bytes: ByteString

    /**
     * Represents the body of a [StompFrame] that was received as a textual web socket frame.
     */
    data class Text(val text: String) : FrameBody() {
        constructor(utf8Bytes: ByteString) : this(utf8Bytes.decodeToString())

        // Text frames must be encoded using UTF-8 as per WebSocket specification
        // If other encodings are needed, the application must use binary frames with relevant content-type header
        override val bytes by lazy { text.encodeToByteString() }
    }

    /**
     * Represents the body of a [StompFrame] that was received as a binary web socket frame.
     */
    data class Binary(override val bytes: ByteString) : FrameBody() {

        internal fun decodeAsText(charset: Charset): String = bytes.decodeToString(charset)
    }
}
