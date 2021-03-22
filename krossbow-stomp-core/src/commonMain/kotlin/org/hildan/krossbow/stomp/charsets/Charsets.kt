package org.hildan.krossbow.stomp.charsets

expect abstract class Charset {
    abstract fun newEncoder(): CharsetEncoder
    abstract fun newDecoder(): CharsetDecoder
}

internal expect fun String.toCharset(): Charset

expect abstract class CharsetEncoder
expect abstract class CharsetDecoder

expect object Charsets {
    val UTF_8: Charset
    val ISO_8859_1: Charset
}

internal expect fun CharsetEncoder.encode(input: String): ByteArray

internal expect fun CharsetDecoder.decode(bytes: ByteArray): String

internal fun extractCharset(mimeTypeText: String): Charset? = mimeTypeText.splitToSequence(';')
    .drop(1)
    .map { it.substringAfter("charset=", "") }
    .firstOrNull { it.isNotEmpty() }
    ?.toCharset()