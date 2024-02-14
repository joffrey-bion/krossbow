package org.hildan.krossbow.io

import kotlinx.io.bytestring.*
import kotlinx.io.bytestring.unsafe.*
import java.nio.*

/**
 * Exposes the contents of this [ByteString] as a read-only [ByteBuffer] without copying data.
 */
@InternalKrossbowIoApi
@OptIn(UnsafeByteStringApi::class)
fun ByteString.asReadOnlyByteBuffer(): ByteBuffer = ByteBuffer.wrap(unsafeBackingByteArray()).asReadOnlyBuffer()

/**
 * Reads all remaining bytes in this [ByteBuffer] into a new [ByteString].
 */
@InternalKrossbowIoApi
@OptIn(UnsafeByteStringApi::class)
fun ByteBuffer.readByteString(): ByteString = readByteArray().asByteString()

/**
 * Reads all remaining bytes in this [ByteBuffer] into a new [ByteArray].
 */
@InternalKrossbowIoApi
private fun ByteBuffer.readByteArray(): ByteArray {
    val array = ByteArray(remaining())
    get(array)
    return array
}
