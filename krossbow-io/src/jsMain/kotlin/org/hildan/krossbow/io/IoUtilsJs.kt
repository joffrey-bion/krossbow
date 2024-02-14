package org.hildan.krossbow.io

import kotlinx.io.*
import kotlinx.io.bytestring.*
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
@InternalKrossbowIoApi
fun ArrayBuffer.toByteString(): ByteString = Buffer().apply { write(this@toByteString) }.readByteString()

private fun Sink.write(buffer: ArrayBuffer) = writeInt8Array(Int8Array(buffer))

private fun Sink.writeInt8Array(array: Int8Array) {
    for (i in 0..<array.length) {
        writeByte(array[i])
    }
}
