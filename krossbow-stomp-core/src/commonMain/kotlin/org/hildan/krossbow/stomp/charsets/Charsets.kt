package org.hildan.krossbow.stomp.charsets

expect abstract class Charset

expect object Charsets {
    val UTF_8: Charset
}

internal expect fun String.encodeToBytes(charset: Charset): ByteArray

internal expect fun ByteArray.decodeToString(charset: Charset): String

internal fun extractCharset(mimeTypeText: String): Charset? = mimeTypeText.splitToSequence(';')
    .drop(1)
    .map { it.substringAfter("charset=", "") }
    .firstOrNull { it.isNotEmpty() }
    ?.toCharset()

internal expect fun String.toCharset(): Charset
