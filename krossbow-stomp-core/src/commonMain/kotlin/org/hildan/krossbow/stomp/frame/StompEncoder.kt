package org.hildan.krossbow.stomp.frame

import kotlinx.io.*
import kotlinx.io.bytestring.*
import org.hildan.krossbow.stomp.headers.HeaderEscaper

internal fun StompFrame.encodeToByteString(): ByteString {
    val buffer = Buffer()
    buffer.writeStompFrame(this)
    return buffer.readByteString()
}

private fun Sink.writeStompFrame(stompFrame: StompFrame) {
    // the preamble has to be encoded in UTF-8 as per the specification
    writeString(stompFrame.preambleText)
    stompFrame.body?.bytes?.let { write(it) }
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
