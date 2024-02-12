package org.hildan.krossbow.stomp.charsets

import kotlinx.io.*
import kotlinx.io.bytestring.*
import java.nio.ByteBuffer
import java.nio.charset.Charset

actual typealias Charset = Charset

actual typealias Charsets = kotlin.text.Charsets

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal actual fun String.encodeToByteString(charset: Charset): ByteString =
    ByteString((this as java.lang.String).getBytes(charset))

internal actual fun ByteString.decodeToString(charset: Charset): String =
    Buffer().apply { write(this@decodeToString) }.readString(charset)

internal actual fun String.toCharset(): Charset = Charset.forName(this)
