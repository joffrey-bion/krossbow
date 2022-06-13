package org.hildan.krossbow.stomp.charsets

actual abstract class Charset(internal val _name: String)

private class CharsetImpl(_name: String): Charset(_name)

actual object Charsets {
    actual val UTF_8: Charset = CharsetImpl("UTF-8")
}

internal actual fun String.encodeToBytes(charset: Charset): ByteArray = when (charset) {
    Charsets.UTF_8 -> encodeToByteArray()
    else -> error("Non UTF-8 encodings are not supported on native platforms")
}

internal actual fun ByteArray.decodeToString(charset: Charset): String = when (charset) {
    Charsets.UTF_8 -> decodeToString()
    else -> error("Non UTF-8 encodings are not supported on native platforms")
}

internal actual fun String.toCharset() = when (lowercase().replace('_', '-')) {
    "utf-8", "utf8" -> Charsets.UTF_8
    else -> throw IllegalArgumentException("Charset $this is not supported on native platforms")
}
