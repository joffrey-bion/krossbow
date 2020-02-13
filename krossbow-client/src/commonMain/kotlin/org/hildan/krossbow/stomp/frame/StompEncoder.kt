package org.hildan.krossbow.stomp.frame

import kotlinx.io.ByteArrayOutputStream
import org.hildan.krossbow.stomp.headers.HeaderEscaper

@UseExperimental(ExperimentalStdlibApi::class)
fun StompFrame.encodeToBytes(): ByteArray {
    val os = ByteArrayOutputStream()
    os.write(encodedPreamble.encodeToByteArray())
    os.write(body.encodeToBytes())
    os.write(0)
    return os.toByteArray()
}

fun StompFrame.encodeToText(): String = "$encodedPreamble${body.encodeToText()}\u0000"

@UseExperimental(ExperimentalStdlibApi::class)
private fun FrameBody?.encodeToBytes(): ByteArray = when(this) {
    is FrameBody.Text -> text.encodeToByteArray()
    is FrameBody.Binary -> rawBytes
    null -> ByteArray(0)
}

@UseExperimental(ExperimentalStdlibApi::class)
private fun FrameBody?.encodeToText(): String = when(this) {
    is FrameBody.Text -> text
    is FrameBody.Binary -> rawBytes.decodeToString()
    null -> ""
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
