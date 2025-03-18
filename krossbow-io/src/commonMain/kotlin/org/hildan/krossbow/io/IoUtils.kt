package org.hildan.krossbow.io

import kotlinx.io.bytestring.*
import kotlinx.io.bytestring.unsafe.*

/**
 * Declarations annotated with this annotation are not meant for public use and may be changed or removed at any time
 * without major version bump.
 *
 * The `krossbow-io` module is an internal module only intended for use within Krossbow itself.
 */
@RequiresOptIn("This is an internal Krossbow API for IO conversions and may be removed at any time")
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
internal annotation class InternalKrossbowIoApi

/**
 * Returns the backing [ByteArray] of this [ByteString], without copying data.
 *
 * Warning: the returned [ByteArray] must not be modified for the lifetime of this [ByteString].
 * If this cannot be guaranteed, use [ByteString.toByteArray] instead.
 */
@InternalKrossbowIoApi
@UnsafeByteStringApi
fun ByteString.unsafeBackingByteArray(): ByteArray {
    val backingByteArray: ByteArray
    UnsafeByteStringOperations.withByteArrayUnsafe(this) {
        backingByteArray = it
    }
    return backingByteArray
}

/**
 * Returns a [ByteString] view of this [ByteArray], without copying data.
 *
 * Warning: this [ByteArray] must not be modified for the lifetime of the returned [ByteString].
 * If this cannot be guaranteed, use the [ByteString] constructor instead.
 */
@InternalKrossbowIoApi
@UnsafeByteStringApi
fun ByteArray.asByteString(): ByteString = UnsafeByteStringOperations.wrapUnsafe(this)
