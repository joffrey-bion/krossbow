package org.hildan.krossbow.stomp.frame

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.Input
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.readUTF8Line
import kotlinx.io.core.readUntilDelimiter
import kotlinx.io.core.use
import org.hildan.krossbow.stomp.headers.HeaderEscaper
import org.hildan.krossbow.stomp.headers.StompHeaders
import org.hildan.krossbow.stomp.headers.asStompHeaders

internal object StompDecoder {

    private const val NULL_BYTE = 0.toByte()

    private val MAX_COMMAND_LENGTH = StompCommand.values().map { it.text.length }.max()!!

    fun decode(frameBytes: ByteArray): StompFrame = ByteReadPacket(frameBytes).use { it.readStompFrame(true) }

    fun decode(frameText: CharSequence): StompFrame = buildPacket { append(frameText) }.use { it.readStompFrame(false) }

    private fun Input.readStompFrame(isBinary: Boolean): StompFrame {
        try {
            val command = readStompCommand()
            val headers = readStompHeaders(command.supportsHeaderEscapes)
            val body = readBodyBytes(headers.contentLength)?.toFrameBody(isBinary)
            expectNullOctet()
            return StompFrame.create(command, headers, body)
        } catch (e: Exception) {
            throw InvalidStompFrameException(e)
        }
    }

    private fun Input.readStompCommand(): StompCommand {
        val firstLine = readUTF8Line(estimate = MAX_COMMAND_LENGTH) ?: error("Missing command in STOMP frame")
        return StompCommand.parse(firstLine)
    }

    private fun Input.readStompHeaders(shouldUnescapeHeaders: Boolean): StompHeaders =
            utf8LineSequence()
                .takeWhile { it.isNotEmpty() } // empty line marks end of headers
                .parseLinesAsStompHeaders(shouldUnescapeHeaders)

    private fun Sequence<String>.parseLinesAsStompHeaders(shouldUnescapeHeaders: Boolean): StompHeaders {
        val headersMap = mutableMapOf<String, String>()
        forEach { line ->
            // the colon ':' is safe to use to split the line because it is escaped as \c (see HeaderEscaper)
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

    private fun Input.utf8LineSequence(): Sequence<String> = sequence {
        while (true) {
            val line = readUTF8Line(estimate = 16) ?: error("Unexpected end of input")
            yield(line)
        }
    }

    private fun Input.readBodyBytes(contentLength: Int?) = when (contentLength) {
        0 -> null
        null -> readUntilNullByte()
        else -> readBytes(contentLength)
    }

    private fun Input.readUntilNullByte() = buildPacket { readUntilDelimiter(NULL_BYTE, this) }.readBytes()

    private fun ByteArray.toFrameBody(binary: Boolean) = if (binary) {
        FrameBody.Binary(this)
    } else {
        FrameBody.Text(this)
    }

    private fun Input.expectNullOctet() {
        val byte = readByte()
        if (byte != NULL_BYTE) {
            throw IllegalStateException("Expected NULL byte at end of frame")
        }
    }
}

class InvalidStompFrameException(cause: Throwable) : Exception("Failed to decode invalid STOMP frame", cause)
