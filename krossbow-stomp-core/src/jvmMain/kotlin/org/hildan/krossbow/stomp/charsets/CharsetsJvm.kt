package org.hildan.krossbow.stomp.charsets

import kotlinx.io.bytestring.*
import kotlinx.io.bytestring.unsafe.*
import org.hildan.krossbow.io.*
import java.nio.charset.Charset

actual typealias Charset = Charset

actual typealias Charsets = kotlin.text.Charsets

@OptIn(UnsafeByteStringApi::class)
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal actual fun String.encodeToByteString(charset: Charset): ByteString =
    (this as java.lang.String).getBytes(charset).asByteString()

@OptIn(UnsafeByteStringApi::class)
internal actual fun ByteString.decodeToString(charset: Charset): String = String(unsafeBackingByteArray(), charset)

internal actual fun String.toCharset(): Charset = Charset.forName(this)
