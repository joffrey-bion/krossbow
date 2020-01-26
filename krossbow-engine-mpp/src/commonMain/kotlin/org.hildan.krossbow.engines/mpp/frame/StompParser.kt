package org.hildan.krossbow.engines.mpp.frame

import org.hildan.krossbow.engines.mpp.headers.StompConnectHeaders
import org.hildan.krossbow.engines.mpp.headers.StompConnectedHeaders
import org.hildan.krossbow.engines.mpp.headers.StompDisconnectHeaders
import org.hildan.krossbow.engines.mpp.headers.StompHeader
import org.hildan.krossbow.engines.mpp.headers.StompHeaders
import org.hildan.krossbow.engines.mpp.headers.StompSendHeaders
import org.hildan.krossbow.engines.mpp.headers.toHeaders

object StompParser {

    fun parse(frameText: String): StompFrame {
        val lines = frameText.lines()
        val command = lines[0]

        val headerLines = lines.drop(1).takeWhile { it.isNotEmpty() }
        val headers = headerLines.map {
            parseHeader(
                it
            )
        }.aggregate()

        val bodyLines = lines.drop(2 + headerLines.size).takeIf { it.isNotEmpty() }
        val body = bodyLines?.joinToString("\n")

        return UntypedStompFrame(command, headers, body).typed()
    }

    private fun parseHeader(header: String): RawStompHeader {
        val (key, value) = header.split(':', ignoreCase = false, limit = 2)
        return RawStompHeader(key, value)
    }
}

data class UntypedStompFrame(
    val command: String,
    val headers: StompHeaders,
    val body: String? = null
) {
    fun typed(): StompFrame {
        return when (command) {
            StompCommands.CONNECT -> StompFrame.Connect(
                StompConnectHeaders(headers)
            )
            StompCommands.CONNECTED -> StompFrame.Connected(
                StompConnectedHeaders(headers)
            )
            StompCommands.DISCONNECT -> StompFrame.Disconnect(
                StompDisconnectHeaders(headers)
            )
            StompCommands.SEND -> StompFrame.Send(
                StompSendHeaders(headers), body
            )
            else -> TODO("command $command not implemented")
        }
    }
}

private data class RawStompHeader(
    val key: String,
    val value: String
)

private fun List<RawStompHeader>.aggregate(): StompHeaders =
    groupBy { it.key }.mapValues { (k, vals) ->
        StompHeader(key = k,
            value = vals[0].value,
            formerValues = vals.drop(1).map { it.value })
    }.toHeaders()
