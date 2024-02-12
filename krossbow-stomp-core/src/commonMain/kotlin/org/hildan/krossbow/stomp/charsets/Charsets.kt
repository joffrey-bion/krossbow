package org.hildan.krossbow.stomp.charsets

import kotlinx.io.bytestring.*

expect abstract class Charset

expect object Charsets {
    val UTF_8: Charset
}

internal expect fun String.encodeToByteString(charset: Charset): ByteString

internal expect fun ByteString.decodeToString(charset: Charset): String

internal fun extractCharset(mimeTypeText: String): Charset? = mimeTypeText.splitToSequence(';')
    .drop(1)
    .map { it.substringAfter("charset=", "") }
    .firstOrNull { it.isNotEmpty() }
    ?.toCharset()

internal expect fun String.toCharset(): Charset
