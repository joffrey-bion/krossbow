package org.hildan.krossbow.stomp.frame

import kotlinx.io.ByteArrayOutputStream
import org.hildan.krossbow.stomp.headers.HeaderEscaper

@UseExperimental(ExperimentalStdlibApi::class)
internal fun StompFrame.encodeToBytes(): ByteArray {
    val os = ByteArrayOutputStream()
    os.write(encodedPreamble.encodeToByteArray())

    body?.bytes?.let { os.write(it) }

    os.write(0)
    return os.toByteArray()
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
