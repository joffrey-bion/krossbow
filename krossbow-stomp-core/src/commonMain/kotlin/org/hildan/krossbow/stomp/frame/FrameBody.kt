package org.hildan.krossbow.stomp.frame

import kotlinx.io.charsets.Charset
import kotlinx.io.charsets.decode
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.ExperimentalIoApi

@OptIn(ExperimentalStdlibApi::class)
sealed class FrameBody {

    abstract val bytes: ByteArray

    data class Text(
        val text: String
    ) : FrameBody() {
        constructor(utf8Bytes: ByteArray) : this(utf8Bytes.decodeToString())

        // Text frames must be encoded using UTF-8 as per WebSocket specification
        // If other encodings are needed, the application must use binary frames with relevant content-type header
        override val bytes by lazy { text.encodeToByteArray() }
    }

    data class Binary(
        override val bytes: ByteArray
    ) : FrameBody() {

        @OptIn(ExperimentalIoApi::class)
        fun decodeAsText(charset: Charset): String = charset.newDecoder().decode(ByteReadPacket(bytes))

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
