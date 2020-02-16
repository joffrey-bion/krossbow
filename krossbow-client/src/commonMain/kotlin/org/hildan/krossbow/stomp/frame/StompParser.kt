package org.hildan.krossbow.stomp.frame

import kotlinx.io.core.IoBuffer
import kotlinx.io.core.readBytes
import kotlinx.io.core.readUTF8Line
import kotlinx.io.core.readUntilDelimiter
import kotlinx.io.core.writeFully
import org.hildan.krossbow.stomp.headers.HeaderEscaper
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.stomp.headers.StompErrorHeaders
import org.hildan.krossbow.stomp.headers.StompHeaders
import org.hildan.krossbow.stomp.headers.StompMessageHeaders
import org.hildan.krossbow.stomp.headers.StompReceiptHeaders
import org.hildan.krossbow.stomp.headers.asStompHeaders

object StompParser {

    fun parse(frameBytes: ByteArray): StompFrame {
        val buffer = IoBuffer.Pool.borrow()
        buffer.writeFully(frameBytes)

        val command = buffer.readStompCommand()
        val headers = buffer.readStompHeaders(command.supportsHeaderEscapes)
        val body = buffer.readBinaryBody(headers.contentLength)

        buffer.release(IoBuffer.Pool)
        return createFrame(command, headers, body)
    }

    private fun IoBuffer.readStompCommand(): StompCommand {
        val firstLine = readUTF8Line(estimate = 16) ?: error("Missing command in STOMP frame")
        return StompCommand.parse(firstLine)
    }

    private fun IoBuffer.readStompHeaders(shouldUnescapeHeaders: Boolean): StompHeaders =
            utf8LineSequence()
                .takeWhile { it.isNotEmpty() }
                .asIterable()
                .parseLinesAsStompHeaders(shouldUnescapeHeaders)

    private fun IoBuffer.utf8LineSequence(): Sequence<String> = sequence {
        while (true) {
            val line = readUTF8Line(estimate = 16) ?: error("Unexpected end of input")
            yield(line)
        }
    }

    private fun IoBuffer.readBinaryBody(contentLength: Int?) = when (contentLength) {
        0 -> null
        null -> FrameBody.Binary(readUpToNullCharacter())
        else -> FrameBody.Binary(readBytes(contentLength))
    }

    private fun IoBuffer.readUpToNullCharacter(): ByteArray {
        val outBuffer = IoBuffer.Pool.borrow()
        outBuffer.resetForWrite()
        readUntilDelimiter(0.toByte(), outBuffer)
        val readBytes = outBuffer.readBytes()
        outBuffer.release(IoBuffer.Pool)
        return readBytes
    }

    fun parse(frameText: String): StompFrame {

        val lines = frameText.lines()
        val command = StompCommand.parse(lines[0])

        val emptyLineIndex = lines.indexOf("")
        if (emptyLineIndex == -1) {
            error("Malformed frame, expected empty line to separate headers from body")
        }

        val headers = lines.subList(1, emptyLineIndex).parseLinesAsStompHeaders(command.supportsHeaderEscapes)

        // TODO stop at content-length if specified in the headers
        val bodyLines = lines.subList(emptyLineIndex + 1, lines.size)
        val body = createBody(bodyLines)

        return createFrame(command, headers, body)
    }

    private fun createBody(bodyLines: List<String>): FrameBody.Text? {
        // the frame must be ended by a NULL octet, and may contain optional new lines
        // https://stomp.github.io/stomp-specification-1.2.html#STOMP_Frames
        return bodyLines.takeIf { it.isNotEmpty() }
            ?.joinToString("\n")
            ?.trimEnd { it == '\n' || it == '\r' }
            ?.trimEnd { it == '\u0000' }
            ?.let { FrameBody.Text(it) }
    }

    private fun createFrame(
        command: StompCommand,
        headers: StompHeaders,
        body: FrameBody?
    ): StompFrame = when (command) {
        StompCommand.CONNECTED -> StompFrame.Connected(StompConnectedHeaders(headers))
        StompCommand.MESSAGE -> StompFrame.Message(StompMessageHeaders(headers), body)
        StompCommand.RECEIPT -> StompFrame.Receipt(StompReceiptHeaders(headers))
        StompCommand.ERROR -> StompFrame.Error(StompErrorHeaders(headers), body)
        else -> error("Unsupported server frame command '$command'")
    }

    private fun Iterable<String>.parseLinesAsStompHeaders(shouldUnescapeHeaders: Boolean): StompHeaders {
        val headersMap = mutableMapOf<String, String>()
        forEach { line ->
            val (rawKey, rawValue) = line.split(':', ignoreCase = false, limit = 2)
            val key = if (shouldUnescapeHeaders) HeaderEscaper.unescape(rawKey) else rawKey
            val value = if (shouldUnescapeHeaders) HeaderEscaper.unescape(rawValue) else rawValue
            // If a client or a server receives repeated frame header entries, only the first header entry SHOULD be
            // used as the value of header entry. Subsequent values are only used to maintain a history of state changes
            // of the header and MAY be ignored.
            // https://stomp.github.io/stomp-specification-1.2.html#Repeated_Header_Entries
            if (!headersMap.containsKey(key)) {
                headersMap[key] = value
            }
        }
        return headersMap.asStompHeaders()
    }
}
