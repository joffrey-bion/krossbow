package org.hildan.krossbow.stomp.frame

import org.hildan.krossbow.stomp.headers.HeaderEscaper
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.stomp.headers.StompErrorHeaders
import org.hildan.krossbow.stomp.headers.StompHeaders
import org.hildan.krossbow.stomp.headers.StompMessageHeaders
import org.hildan.krossbow.stomp.headers.StompReceiptHeaders
import org.hildan.krossbow.stomp.headers.asStompHeaders

object StompParser {

    @UseExperimental(ExperimentalStdlibApi::class)
    fun parse(frameBytes: ByteArray): StompFrame {
        // TODO handle binary body properly (scan the bytes progressively to avoid decoding the body)
        // TODO handle content-length header to read bodies with null bytes
        // TODO handle content-type header and respect mime type to decode the text or keep the binary blob
        return parse(frameBytes.decodeToString())
    }

    fun parse(frameText: String): StompFrame {

        val lines = frameText.lines()
        val command = StompCommand.parse(lines[0])

        val headerLines = lines.drop(1).takeWhile { it.isNotEmpty() }
        val headers = parseHeaders(headerLines, command.supportsHeaderEscapes)

        val bodyLines = lines.drop(2 + headerLines.size).takeIf { it.isNotEmpty() }
        val body = bodyLines?.joinToString("\n")?.let { FrameBody.Text(it) }

        return when (command) {
            StompCommand.CONNECTED -> StompFrame.Connected(StompConnectedHeaders(headers))
            StompCommand.MESSAGE -> StompFrame.Message(StompMessageHeaders(headers), body)
            StompCommand.RECEIPT -> StompFrame.Receipt(StompReceiptHeaders(headers))
            StompCommand.ERROR -> StompFrame.Error(StompErrorHeaders(headers), body)
            else -> error("Unsupported server frame command '$command'")
        }
    }

    private fun parseHeaders(headerLines: List<String>, shouldUnescapeHeaders: Boolean): StompHeaders {
        val headersMap = mutableMapOf<String, String>()
        headerLines.forEach { line ->
            val (rawKey, rawValue) = line.split(':', ignoreCase = false, limit = 2)
            val key = if (shouldUnescapeHeaders) HeaderEscaper.unescape(rawKey) else rawKey
            val value = if (shouldUnescapeHeaders) HeaderEscaper.unescape(rawValue) else rawValue
            // If a client or a server receives repeated frame header entries, only the first header entry SHOULD be
            // used as the value of header entry. Subsequent values are only used to maintain a history of state changes
            // of the header and MAY be ignored.
            // https://stomp.github.io/stomp-specification-1.2.html#Repeated_Header_Entries
            headersMap.putIfAbsent(key, value)
        }
        return headersMap.asStompHeaders()
    }
}

private fun MutableMap<String, String>.putIfAbsent(key: String, value: String) {
    getOrPut(key) { value }
}
