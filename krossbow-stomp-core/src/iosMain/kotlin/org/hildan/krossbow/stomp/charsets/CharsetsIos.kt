package org.hildan.krossbow.stomp.charsets

actual abstract class Charset(internal val _name: String) {
    actual abstract fun newEncoder(): CharsetEncoder
    actual abstract fun newDecoder(): CharsetDecoder
}

internal data class CharsetImpl(val name: String) : Charset(name) {
    override fun newEncoder(): CharsetEncoder = CharsetEncoderImpl(this)
    override fun newDecoder(): CharsetDecoder = CharsetDecoderImpl(this)
}

internal actual fun String.toCharset() = when (lowercase().replace('_', '-')) {
    "utf-8", "utf8" -> Charsets.UTF_8
    "iso-8859-1", "latin1" -> Charsets.ISO_8859_1
    else -> throw IllegalArgumentException("Charset $this is not supported")
}

actual abstract class CharsetDecoder(internal val _charset: Charset)
actual abstract class CharsetEncoder(internal val _charset: Charset)

internal data class CharsetDecoderImpl(private val charset: Charset) : CharsetDecoder(charset)
internal data class CharsetEncoderImpl(private val charset: Charset) : CharsetEncoder(charset)

actual object Charsets {
    actual val UTF_8: Charset = CharsetImpl("UTF-8")
    actual val ISO_8859_1: Charset = CharsetImpl("ISO-8859-1")
}

actual fun CharsetEncoder.encode(input: String): ByteArray = when (_charset) {
    Charsets.UTF_8 -> input.encodeToByteArray()
    else -> error("Non UTF-8 encodings are not supported on iOS platform")
}

actual fun CharsetDecoder.decode(bytes: ByteArray): String = when (_charset) {
    Charsets.UTF_8 -> bytes.decodeToString()
    else -> error("Non UTF-8 encodings are not supported on iOS platform")
}
