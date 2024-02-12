package org.hildan.krossbow.stomp.charsets

import kotlinx.io.bytestring.*

actual abstract class Charset(internal val _name: String)

private class CharsetImpl(_name: String): Charset(_name)

actual object Charsets {
    actual val UTF_8: Charset = CharsetImpl("UTF-8")
}

internal actual fun String.encodeToByteString(charset: Charset): ByteString = when (charset) {
    Charsets.UTF_8 -> encodeToByteString()
    else -> error("Non UTF-8 encodings are not supported on native platforms")
}

internal actual fun ByteString.decodeToString(charset: Charset): String = when (charset) {
    Charsets.UTF_8 -> decodeToString()
    else -> error("Non UTF-8 encodings are not supported on native platforms")
}

internal actual fun String.toCharset() = when (lowercase().replace('_', '-')) {
    "utf-8", "utf8" -> Charsets.UTF_8
    else -> throw IllegalArgumentException("Charset $this is not supported on native platforms")
}
