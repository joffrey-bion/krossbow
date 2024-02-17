package org.hildan.krossbow.io

import kotlinx.cinterop.*
import kotlinx.io.bytestring.*
import kotlinx.io.bytestring.unsafe.*
import platform.Foundation.*
import platform.posix.*

/**
 * Creates a new [NSData] containing the data copied from this [ByteString].
 */
@InternalKrossbowIoApi
@OptIn(UnsafeByteStringApi::class)
fun ByteString.toNSData(): NSData = unsafeBackingByteArray().toNSData()

/**
 * Creates a new [NSData] containing the data copied from this [ByteArray].
 */
@InternalKrossbowIoApi
@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.convert())
}

/**
 * Creates a new [ByteString] containing the data copied from this [NSData].
 */
@InternalKrossbowIoApi
@OptIn(UnsafeByteStringApi::class)
fun NSData.toByteString(): ByteString = toByteArray().asByteString()

/**
 * Creates a new [ByteArray] containing the data copied from this [NSData].
 */
@InternalKrossbowIoApi
@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
private fun NSData.toByteArray(): ByteArray {
    // length=0 breaks memcpy for some reason (ArrayIndexOutOfBoundsException)
    // and it doesn't hurt to skip memcpy anyway if the array is empty
    if (length.toInt() == 0) return ByteArray(0)

    val nsData = this
    return ByteArray(nsData.length.toInt()).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
        }
    }
}
