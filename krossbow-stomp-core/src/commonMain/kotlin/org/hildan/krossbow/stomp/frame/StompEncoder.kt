package org.hildan.krossbow.stomp.frame

import kotlinx.io.charsets.Charsets
import kotlinx.io.core.Output
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully
import kotlinx.io.core.writeText
import org.hildan.krossbow.stomp.headers.HeaderEscaper

internal fun StompFrame.encodeToBytes(): ByteArray = buildPacket { writeStompFrame(this@encodeToBytes) }.readBytes()

private fun Output.writeStompFrame(stompFrame: StompFrame) {
    // the preamble has to be encoded in UTF-8 as per the specification
    writeText(stompFrame.preambleText, charset = Charsets.UTF_8)
    stompFrame.body?.bytes?.let { writeFully(it) }
    writeByte(0)
}

internal fun StompFrame.encodeToText(): String = "$preambleText${body.encodeToText()}\u0000"

private fun FrameBody?.encodeToText(): String = when (this) {
    null -> ""
    is FrameBody.Binary -> throw IllegalArgumentException("Cannot encode text frame with binary body")
    is FrameBody.Text -> text
}

private val StompFrame.preambleText: String
    get() = buildString {
        append(command.text)
        append('\n')
        headers.forEach { (name, value) ->
            append(maybeEscape(name))
            append(':')
            append(maybeEscape(value))
            append('\n')
        }
        append('\n') // additional empty line to separate the preamble from the body
    }

private fun StompFrame.maybeEscape(s: String) = if (command.supportsHeaderEscapes) HeaderEscaper.escape(s) else s
