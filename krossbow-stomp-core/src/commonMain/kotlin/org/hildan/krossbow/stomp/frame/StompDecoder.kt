package org.hildan.krossbow.stomp.frame

import kotlinx.io.*
import kotlinx.io.bytestring.*
import org.hildan.krossbow.io.*
import org.hildan.krossbow.stomp.headers.HeaderEscaper
import org.hildan.krossbow.stomp.headers.StompHeaders
import org.hildan.krossbow.stomp.headers.asStompHeaders

private const val NULL_BYTE: Byte = 0

internal fun ByteString.decodeToStompFrame() = toSource().readStompFrame(isBinary = true)

internal fun String.decodeToStompFrame() = toSource().readStompFrame(isBinary = false)

private fun Source.readStompFrame(isBinary: Boolean): StompFrame {
    try {
        val command = readStompCommand()
        val headers = readStompHeaders(command.supportsHeaderEscapes)
        val body = readBodyBytes(headers.contentLength)?.toFrameBody(isBinary)
        expectNullOctet()
        expectOnlyEOLs()
        return StompFrame.create(command, headers, body)
    } catch (e: Exception) {
        throw InvalidStompFrameException(e)
    }
}

private fun Source.readStompCommand(): StompCommand {
    val firstLine = readLine() ?: error("Missing command in STOMP frame")
    return StompCommand.parse(firstLine)
}

private fun Source.readStompHeaders(shouldUnescapeHeaders: Boolean): StompHeaders =
    generateSequence { readLineStrict() }
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

private fun Source.readBodyBytes(contentLength: Int?) = when (contentLength) {
    0 -> null
    else -> readByteString(contentLength ?: indexOf(NULL_BYTE).toInt())
}

private fun ByteString.toFrameBody(binary: Boolean) = when {
    isEmpty() -> null
    binary -> FrameBody.Binary(this)
    else -> FrameBody.Text(this)
}

private fun Source.expectNullOctet() {
    if (readByte() != NULL_BYTE) {
        error("Expected NULL byte at end of frame")
    }
}

private fun Source.expectOnlyEOLs() {
    if (!exhausted()) {
        val endText = readString()
        if (endText.any { it != '\n' && it != '\r' }) {
            error("Unexpected non-EOL characters after end-of-frame NULL character: $endText")
        }
    }
}

/**
 * Exception thrown when some frame data could not be decoded as a STOMP frame.
 */
class InvalidStompFrameException(cause: Throwable) : Exception("Failed to decode invalid STOMP frame", cause)
