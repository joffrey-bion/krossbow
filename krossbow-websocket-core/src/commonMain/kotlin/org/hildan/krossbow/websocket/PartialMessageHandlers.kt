package org.hildan.krossbow.websocket

import kotlinx.io.*
import kotlinx.io.bytestring.*

internal abstract class PartialMessageHandler<T>(
    private val onMessageComplete: suspend (T) -> Unit,
) {
    private val buffer = Buffer()

    suspend fun processMessage(frameData: T, isLast: Boolean = true) {
        // for a Buffer, exhausted() really just means "is empty right now" and never blocks
        if (buffer.exhausted() && isLast) {
            // optimization: do not buffer single-part messages
            onMessageComplete(frameData)
        } else {
            buffer.writePartialMessage(frameData = frameData)
            if (isLast) {
                val fullMsg = buffer.readCompleteMessage()
                onMessageComplete(fullMsg)
            }
        }
    }

    suspend fun processMessage(isLast: Boolean = true, writeData: Sink.() -> Unit) {
        buffer.writeData()
        if (isLast) {
            onMessageComplete(buffer.readCompleteMessage())
        }
    }

    abstract fun Sink.writePartialMessage(frameData: T)
    abstract fun Source.readCompleteMessage(): T

    fun close() {
        buffer.close()
    }
}

internal class PartialTextMessageHandler(
    onMessageComplete: suspend (String) -> Unit,
) : PartialMessageHandler<String>(onMessageComplete) {

    override fun Sink.writePartialMessage(frameData: String) = writeString(frameData)

    override fun Source.readCompleteMessage(): String = readString()
}

internal class PartialBinaryMessageHandler(
    onMessageComplete: suspend (ByteString) -> Unit,
) : PartialMessageHandler<ByteString>(onMessageComplete) {

    override fun Sink.writePartialMessage(frameData: ByteString) = write(frameData)

    override fun Source.readCompleteMessage(): ByteString = readByteString()
}
