package org.hildan.krossbow.engines.mpp.frame

import org.hildan.krossbow.engines.mpp.headers.StompConnectHeaders
import org.hildan.krossbow.engines.mpp.headers.StompConnectedHeaders
import org.hildan.krossbow.engines.mpp.headers.StompDisconnectHeaders
import org.hildan.krossbow.engines.mpp.headers.StompHeader
import org.hildan.krossbow.engines.mpp.headers.StompHeaders
import org.hildan.krossbow.engines.mpp.headers.StompSendHeaders
import org.hildan.krossbow.engines.mpp.headers.toHeaders

object StompParser {

    @UseExperimental(ExperimentalStdlibApi::class)
    fun parse(frameBytes: ByteArray): StompFrame = parse(frameBytes.decodeToString())

    fun parse(frameText: String): StompFrame {
        // TODO handle binary body properly
        // TODO handle content-length header to read bodies with null bytes
        // TODO handle content-type header and respect mime type to decode the text or keep the binary blob
        // TODO scan the bytes progressively to avoid decoding the body

        val lines = frameText.lines()
        val command = StompCommand.parse(lines[0])

        val headerLines = lines.drop(1).takeWhile { it.isNotEmpty() }
        val headers = headerLines.map { parseHeader(it) }.aggregate()

        val bodyLines = lines.drop(2 + headerLines.size).takeIf { it.isNotEmpty() }
        val body = bodyLines?.joinToString("\n")?.let { FrameBody.Text(it) }

        return when (command) {
            StompCommand.CONNECT -> StompFrame.Connect(StompConnectHeaders(headers))
            StompCommand.CONNECTED -> StompFrame.Connected(StompConnectedHeaders(headers))
            StompCommand.DISCONNECT -> StompFrame.Disconnect(StompDisconnectHeaders(headers))
            StompCommand.SEND -> StompFrame.Send(StompSendHeaders(headers), body)
            else -> TODO("command $command not implemented")
        }
    }

    private fun parseHeader(header: String): RawStompHeader {
        val (key, value) = header.split(':', ignoreCase = false, limit = 2)
        return RawStompHeader(key, value)
    }
}

private data class RawStompHeader(
    val key: String,
    val value: String
)

// TODO forget about header history, who cares
private fun List<RawStompHeader>.aggregate(): StompHeaders =
    groupBy { it.key }.mapValues { (k, vals) ->
        StompHeader(
            key = k,
            value = vals[0].value,
            formerValues = vals.drop(1).map { it.value }
        )
    }.toHeaders()
