package org.hildan.krossbow.stomp.frame

@OptIn(ExperimentalStdlibApi::class)
sealed class FrameBody {

    abstract val bytes: ByteArray

    data class Text(
        val text: String
    ) : FrameBody() {
        constructor(bytes: ByteArray) : this(bytes.decodeToString())

        // Text frames must be encoded using UTF-8 as per WebSocket specification
        // If other encodings are needed, the application must use binary frames with relevant content-type header
        override val bytes by lazy { text.encodeToByteArray() }
    }

    data class Binary(
        override val bytes: ByteArray
    ) : FrameBody() {

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

@OptIn(ExperimentalStdlibApi::class)
fun FrameBody.asText() = when (this) {
    is FrameBody.Binary -> bytes.decodeToString(throwOnInvalidSequence = true)
    is FrameBody.Text -> text
}
