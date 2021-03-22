package org.hildan.krossbow.stomp.charsets

import java.nio.ByteBuffer

actual typealias Charset = java.nio.charset.Charset

actual typealias CharsetEncoder = java.nio.charset.CharsetEncoder
actual typealias CharsetDecoder = java.nio.charset.CharsetDecoder

actual typealias Charsets = kotlin.text.Charsets

internal actual fun String.toCharset(): Charset = Charset.forName(this)

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
internal actual fun CharsetEncoder.encode(input: String): ByteArray = (input as java.lang.String).getBytes(charset())

internal actual fun CharsetDecoder.decode(bytes: ByteArray): String = buildString { decode(ByteBuffer.wrap(bytes)) }
