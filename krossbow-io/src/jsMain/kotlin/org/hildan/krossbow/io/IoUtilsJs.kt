package org.hildan.krossbow.io

import kotlinx.io.bytestring.*
import kotlinx.io.bytestring.unsafe.*
import org.khronos.webgl.*

/**
 * Creates a new [ArrayBuffer] containing the data copied from this [ByteString].
 */
@InternalKrossbowIoApi
fun ByteString.toArrayBuffer(): ArrayBuffer = toInt8Array().buffer

private fun ByteString.toInt8Array() = Int8Array(toArrayOfBytes())

private fun ByteString.toArrayOfBytes() = Array(size) { this[it] }

/**
 * Creates a new [ByteString] containing the data copied from this [ArrayBuffer].
 */
@OptIn(UnsafeByteStringApi::class)
@InternalKrossbowIoApi
fun ArrayBuffer.toByteString(): ByteString = toByteArray().asByteString()

private fun ArrayBuffer.toByteArray(): ByteArray = Int8Array(this).toByteArray()

private fun Int8Array.toByteArray() = ByteArray(length) { this[it] }
