package org.hildan.krossbow.stomp.frame

import kotlinx.io.*
import kotlinx.io.bytestring.*
import org.hildan.krossbow.stomp.frame.StompFrame.*
import org.hildan.krossbow.stomp.headers.*

private const val NULL_BYTE: Byte = 0

internal fun ByteString.decodeToStompFrame() = toSource().readStompFrame(isBinary = true)

internal fun String.decodeToStompFrame() = toSource().readStompFrame(isBinary = false)

private fun ByteString.toSource(): Source = Buffer().apply { write(this@toSource) }
private fun String.toSource(): Source = Buffer().apply { writeString(this@toSource) }

private fun Source.readStompFrame(isBinary: Boolean): StompFrame {
    try {
        val command = readStompCommand()
        val headers = readStompHeaders(command.supportsHeaderEscapes)
        val body = readBodyBytes(headers.contentLength)?.toFrameBody(isBinary)
        expectNullOctet()
        expectOnlyEOLs()
        return create(command, headers, body)
    } catch (e: Exception) {
        throw InvalidStompFrameException(e)
    }
}

private val Map<String, String>.contentLength: Int?
    get() {
        val headerTextValue = this[HeaderNames.CONTENT_LENGTH] ?: return null
        return headerTextValue.toIntOrNull()
            ?: throw InvalidStompHeaderException("invalid 'content-length' header value '$headerTextValue'")
    }

private fun create(
    command: StompCommand,
    headers: MutableMap<String, String>,
    body: FrameBody?,
): StompFrame = when (command) {
    StompCommand.STOMP -> Stomp(StompConnectHeaders(headers))
    StompCommand.CONNECT -> Connect(StompConnectHeaders(headers))
    StompCommand.CONNECTED -> Connected(StompConnectedHeaders(headers))
    StompCommand.MESSAGE -> Message(StompMessageHeaders(headers), body)
    StompCommand.RECEIPT -> Receipt(StompReceiptHeaders(headers))
    StompCommand.SEND -> Send(StompSendHeaders(headers), body)
    StompCommand.SUBSCRIBE -> Subscribe(StompSubscribeHeaders(headers))
    StompCommand.UNSUBSCRIBE -> Unsubscribe(StompUnsubscribeHeaders(headers))
    StompCommand.ACK -> Ack(StompAckHeaders(headers))
    StompCommand.NACK -> Nack(StompNackHeaders(headers))
    StompCommand.BEGIN -> Begin(StompBeginHeaders(headers))
    StompCommand.COMMIT -> Commit(StompCommitHeaders(headers))
    StompCommand.ABORT -> Abort(StompAbortHeaders(headers))
    StompCommand.DISCONNECT -> Disconnect(StompDisconnectHeaders(headers))
    StompCommand.ERROR -> Error(StompErrorHeaders(headers), body)
}

private fun Source.readStompCommand(): StompCommand {
    val firstLine = readLine() ?: error("Missing command in STOMP frame")
    return StompCommand.parse(firstLine)
}

private fun Source.readStompHeaders(shouldUnescapeHeaders: Boolean): MutableMap<String, String> =
    generateSequence { readLineStrict() }
        .takeWhile { it.isNotEmpty() } // empty line marks end of headers
        .parseLinesAsStompHeaders(shouldUnescapeHeaders)

private fun Sequence<String>.parseLinesAsStompHeaders(shouldUnescapeHeaders: Boolean): MutableMap<String, String> {
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
    return headersMap
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

// ok to be private, it will be wrapped in InvalidStompFrameException anyway
private class InvalidStompHeaderException(message: String) : Exception(message)
