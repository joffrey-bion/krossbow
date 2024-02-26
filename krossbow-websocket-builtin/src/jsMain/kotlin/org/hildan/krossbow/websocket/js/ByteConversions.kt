package org.hildan.krossbow.websocket.js

import kotlinx.io.bytestring.*
import kotlinx.io.bytestring.unsafe.*
import org.hildan.krossbow.io.*
import org.khronos.webgl.*

/**
 * Creates a new [ArrayBuffer] containing the data copied from this [ByteString].
 */
internal fun ByteString.toArrayBuffer(): ArrayBuffer = toInt8Array().buffer

private fun ByteString.toInt8Array() = Int8Array(toArrayOfBytes())

private fun ByteString.toArrayOfBytes() = Array(size) { this[it] }

/**
 * Creates a new [ByteString] containing the data copied from this [ArrayBuffer].
 */
@OptIn(UnsafeByteStringApi::class)
internal fun ArrayBuffer.toByteString(): ByteString = toByteArray().asByteString()

private fun ArrayBuffer.toByteArray(): ByteArray = Int8Array(this).toByteArray()

private fun Int8Array.toByteArray() = ByteArray(length) { this[it] }
