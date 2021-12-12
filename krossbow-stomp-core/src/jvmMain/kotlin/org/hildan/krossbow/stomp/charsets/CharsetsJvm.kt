package org.hildan.krossbow.stomp.charsets

import java.nio.ByteBuffer
import java.nio.charset.Charset

actual typealias Charset = Charset

actual typealias Charsets = kotlin.text.Charsets

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal actual fun String.encodeToBytes(charset: Charset): ByteArray =
    (this as java.lang.String).getBytes(charset.newEncoder().charset())

internal actual fun ByteArray.decodeToString(charset: Charset): String = buildString {
    charset.decode(ByteBuffer.wrap(this@decodeToString))
}

internal actual fun String.toCharset(): Charset = Charset.forName(this)
