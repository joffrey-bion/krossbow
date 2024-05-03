package org.hildan.krossbow.stomp.charsets

import kotlinx.io.bytestring.*

internal actual fun String.encodeToByteString(charset: Charset): ByteString = when (charset) {
    Charset.UTF_8 -> encodeToByteString()
    else -> error("Charset $charset is not supported on native platforms, please use UTF-8")
}

internal actual fun ByteString.decodeToString(charset: Charset): String = when (charset) {
    Charset.UTF_8 -> decodeToString()
    else -> error("Charset $charset is not supported on native platforms, please use UTF-8")
}
