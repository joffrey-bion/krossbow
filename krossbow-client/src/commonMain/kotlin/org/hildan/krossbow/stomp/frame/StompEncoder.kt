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

private fun FrameBody?.encodeToBytes(): ByteArray = asBytes() ?: ByteArray(0)

private fun FrameBody?.encodeToText(): String = asText() ?: ""

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
