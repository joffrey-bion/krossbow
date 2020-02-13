package org.hildan.krossbow.stomp.frame

import org.hildan.krossbow.stomp.headers.HeaderEscaper

// TODO align API with StompFrame.toBytes() (we want them to look similar)
fun StompFrame.format(): String = """
$command
$formattedHeaders

${body ?: ""}
""".trimIndent() + "\u0000"

private val StompFrame.formattedHeaders: String
    get() = headers.entries.joinToString("\n") {
        formatHeader(it.key, it.value, command.supportsHeaderEscapes)
    }

private fun formatHeader(key: String, value: String, escapeContent: Boolean): String =
    if (escapeContent) {
        "${HeaderEscaper.escape(key)}:${HeaderEscaper.escape(value)}"
    } else {
        "$key:$value"
    }
