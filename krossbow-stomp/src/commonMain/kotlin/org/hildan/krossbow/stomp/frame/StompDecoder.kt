package org.hildan.krossbow.stomp.frame

import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.Input
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.readUTF8Line
import kotlinx.io.core.readUntilDelimiter
import kotlinx.io.core.use
import org.hildan.krossbow.stomp.headers.HeaderEscaper
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.stomp.headers.StompDisconnectHeaders
import org.hildan.krossbow.stomp.headers.StompErrorHeaders
import org.hildan.krossbow.stomp.headers.StompHeaders
import org.hildan.krossbow.stomp.headers.StompMessageHeaders
import org.hildan.krossbow.stomp.headers.StompReceiptHeaders
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.stomp.headers.StompUnsubscribeHeaders
import org.hildan.krossbow.stomp.headers.asStompHeaders

object StompDecoder {

    private const val NULL_BYTE = 0.toByte()

    private val MAX_COMMAND_LENGTH = StompCommand.values().map { it.text.length }.max()!!

    fun decode(frameBytes: ByteArray): StompFrame = ByteReadPacket(frameBytes).use { it.readStompFrame() }

    private fun Input.readStompFrame(): StompFrame {
        try {
            val command = readStompCommand()
            val headers = readStompHeaders(command.supportsHeaderEscapes)
            val body = readBinaryBody(headers.contentLength)
            return createFrame(command, headers, body)
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
                .asIterable()
                .parseLinesAsStompHeaders(shouldUnescapeHeaders)

    private fun Input.utf8LineSequence(): Sequence<String> = sequence {
        while (true) {
            val line = readUTF8Line(estimate = 16) ?: error("Unexpected end of input")
            yield(line)
        }
    }

    private fun Input.readBinaryBody(contentLength: Int?) = when (contentLength) {
        0 -> null
        null -> FrameBody.Binary(readUpToNullCharacter())
        else -> FrameBody.Binary(readBytes(contentLength))
    }

    private fun Input.readUpToNullCharacter(): ByteArray =
            buildPacket { readUntilDelimiter(NULL_BYTE, this) }.readBytes()

    fun decode(frameText: String): StompFrame {
        try {
            val lines = frameText.lines()
            val command = StompCommand.parse(lines[0])

            val emptyLineIndex = lines.indexOf("")
            if (emptyLineIndex == -1) {
                error("Malformed frame, expected empty line to separate headers from body")
            }

            val headers = lines.subList(1, emptyLineIndex).parseLinesAsStompHeaders(command.supportsHeaderEscapes)

            // TODO stop at content-length if specified in the headers
            val restOfTheFrame = lines.subList(emptyLineIndex + 1, lines.size).joinToString("\n")

            // the frame must be ended by a NULL octet, which may be followed by optional new lines
            // https://stomp.github.io/stomp-specification-1.2.html#STOMP_Frames
            val bodyText = restOfTheFrame.trimEnd { it == '\n' || it == '\r' }.trimEnd { it == '\u0000' }
            val body = bodyText.ifEmpty { null }?.let { FrameBody.Text(it) }

            return createFrame(command, headers, body)
        } catch (e: Exception) {
            throw InvalidStompFrameException(e)
        }
    }

    private fun createFrame(
        command: StompCommand,
        headers: StompHeaders,
        body: FrameBody?
    ): StompFrame = when (command) {
        StompCommand.CONNECT -> StompFrame.Connect(StompConnectHeaders(headers))
        StompCommand.CONNECTED -> StompFrame.Connected(StompConnectedHeaders(headers))
        StompCommand.MESSAGE -> StompFrame.Message(StompMessageHeaders(headers), body)
        StompCommand.RECEIPT -> StompFrame.Receipt(StompReceiptHeaders(headers))
        StompCommand.SEND -> StompFrame.Send(StompSendHeaders(headers), body)
        StompCommand.SUBSCRIBE -> StompFrame.Subscribe(StompSubscribeHeaders(headers))
        StompCommand.UNSUBSCRIBE -> StompFrame.Unsubscribe(StompUnsubscribeHeaders(headers))
        StompCommand.ACK -> TODO()
        StompCommand.NACK -> TODO()
        StompCommand.BEGIN -> TODO()
        StompCommand.COMMIT -> TODO()
        StompCommand.ABORT -> TODO()
        StompCommand.DISCONNECT -> StompFrame.Disconnect(StompDisconnectHeaders(headers))
        StompCommand.ERROR -> StompFrame.Error(StompErrorHeaders(headers), body)
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

class InvalidStompFrameException(cause: Throwable) : Exception("Failed to decode invalid STOMP frame", cause)
