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
    writeText(stompFrame.encodedPreamble, charset = Charsets.UTF_8)
    stompFrame.body?.bytes?.let { writeFully(it) }
    writeByte(0)
}

internal fun StompFrame.encodeToText(): String = "$encodedPreamble${body.encodeToText()}\u0000"

private fun FrameBody?.encodeToText(): String = when (this) {
    null -> ""
    is FrameBody.Binary -> throw IllegalArgumentException("Cannot encode text frame with binary body")
    is FrameBody.Text -> text
}

private val StompFrame.encodedPreamble: String
    get() {
        val preamble = StringBuilder()
        preamble.append(command.text)
        preamble.append('\n')
        headers.forEach { (k, v) ->
            preamble.append(encodeHeader(k, v, command.supportsHeaderEscapes))
            preamble.append('\n')
        }
        preamble.append('\n') // additional empty line to separate the preamble from the body
        return preamble.toString()
    }

private fun encodeHeader(key: String, value: String, escapeContent: Boolean): String =
    if (escapeContent) {
        "${HeaderEscaper.escape(key)}:${HeaderEscaper.escape(value)}"
    } else {
        "$key:$value"
    }
