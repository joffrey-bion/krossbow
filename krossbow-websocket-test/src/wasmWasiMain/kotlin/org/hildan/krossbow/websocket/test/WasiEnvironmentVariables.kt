package org.hildan.krossbow.websocket.test

import kotlin.wasm.unsafe.*

private data class EnvSizes(
    /**
     * The number of environment variables.
     */
    val environCount: Int,
    /**
     * The size of the environment variable string data.
     */
    val environBufSize: Int,
)

/**
 * WASI function to read environment variables.
 */
@OptIn(UnsafeWasmMemoryApi::class)
internal fun environGet(): Map<String, String> = withScopedMemoryAllocator { allocator ->
    val (environCount, environBufSize) = environSizesGet()
    val environPtr = allocator.allocate(environCount * Int.SIZE_BYTES) // one int-sized pointer per env var
    val environBufPtr = allocator.allocate(environBufSize)
    val resultCode = wasiEnvironGet(
        environ = environPtr.address.toInt(),
        environ_buf = environBufPtr.address.toInt(),
    )
    if (resultCode != 0) {
        error("environ_get returned non-zero code $resultCode")
    }

    buildMap {
        val buffer = ByteArray(environBufSize) // to hold the biggest env vars
        repeat(environCount) { i ->
            val environVarPointer = Pointer((environPtr + i * Int.SIZE_BYTES).loadInt().toUInt())
            val end = readZeroTerminatedByteArray(environVarPointer, buffer)
            val keyValue = buffer.decodeToString(0, end)
            val (key, value) = keyValue.split("=")
            put(key, value)
        }
    }
}

@OptIn(UnsafeWasmMemoryApi::class)
private fun readZeroTerminatedByteArray(ptr: Pointer, byteArray: ByteArray): Int {
    for (i in byteArray.indices) {
        val b = (ptr + i).loadByte()
        if (b.toInt() == 0)
            return i
        byteArray[i] = b
    }
    error("Zero-terminated array is out of bounds")
}

@OptIn(UnsafeWasmMemoryApi::class)
private fun environSizesGet(): EnvSizes = withScopedMemoryAllocator { allocator ->
    val environCountPtr = allocator.allocate(Int.SIZE_BYTES)
    val environBufSizePtr = allocator.allocate(Int.SIZE_BYTES)
    val resultCode = wasiEnvironSizesGet(
        environ_count = environCountPtr.address.toInt(),
        environ_buf_size = environBufSizePtr.address.toInt(),
    )
    if (resultCode != 0) {
        error("environ_sizes_get returned non-zero code $resultCode")
    }
    return EnvSizes(environCount = environCountPtr.loadInt(), environBufSize = environBufSizePtr.loadInt())
}

/**
 * The `environ_get()` function is used to read the environment variable data.
 * It writes the environment variable pointers and string data to the specified buffers.
 * The sizes of the buffers should match the values returned by the `environ_sizes_get()` function.
 *
 * See [environ_get](https://wasix.org/docs/api-reference/wasi/environ_get).
 *
 * @param environ A WebAssembly pointer to a buffer where the environment variable pointers will be written.
 * @param environ_buf A WebAssembly pointer to a buffer where the environment variable string data will be written.
 */
@WasmImport("wasi_snapshot_preview1", "environ_get")
private external fun wasiEnvironGet(environ: Int, environ_buf: Int): Int

/**
 * The environ_sizes_get() function is used to retrieve the sizes of the environment variable data.
 * It returns the number of environment variables and the size of the environment variable string data.
 *
 * See [environ_sizes_get](https://wasix.org/docs/api-reference/wasi/environ_sizes_get).
 *
 * @param environ_count A WebAssembly pointer to a memory location where the number of environment variables will be written.
 * @param environ_buf_size A WebAssembly pointer to a memory location where the size of the environment variable string data will be written.
 */
@WasmImport("wasi_snapshot_preview1", "environ_sizes_get")
private external fun wasiEnvironSizesGet(environ_count: Int, environ_buf_size: Int): Int
