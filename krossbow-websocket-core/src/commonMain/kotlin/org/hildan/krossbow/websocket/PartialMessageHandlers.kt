package org.hildan.krossbow.websocket

import okio.Buffer

internal class PartialBinaryMessageHandler(
    private val onMessageComplete: suspend (ByteArray) -> Unit
) {
    private val buffer = Buffer()

    suspend fun processMessage(bytes: ByteArray, isLast: Boolean = true) {
        processPartialMessage(
            msg = bytes,
            isLast = isLast,
            isBufferEmpty = buffer.exhausted(),
            appendToBuffer = { buffer.write(it) },
            readAndClearBuffer = { buffer.readByteArray() },
            onMessageComplete = { onMessageComplete(it) }
        )
    }

    fun close() {
        buffer.close()
    }
}

internal class PartialTextMessageHandler(
    private val onMessageComplete: suspend (CharSequence) -> Unit
) {
    private val textBuilder = StringBuilder()

    suspend fun processMessage(text: CharSequence, isLast: Boolean = true) {
        processPartialMessage(
            msg = text,
            isLast = isLast,
            isBufferEmpty = textBuilder.isEmpty(),
            appendToBuffer = { textBuilder.append(it) },
            readAndClearBuffer = { textBuilder.toString().also { textBuilder.clear() } },
            onMessageComplete = { onMessageComplete(it) }
        )
    }
}

private inline fun <T> processPartialMessage(
    msg: T,
    isLast: Boolean,
    isBufferEmpty: Boolean,
    appendToBuffer: (T) -> Unit,
    readAndClearBuffer: () -> T,
    onMessageComplete: (T) -> Unit
) {
    if (isBufferEmpty && isLast) {
        // optimization: do not buffer complete messages
        onMessageComplete(msg)
    } else {
        appendToBuffer(msg)
        if (isLast) {
            val fullMsg = readAndClearBuffer()
            onMessageComplete(fullMsg)
        }
    }
}
