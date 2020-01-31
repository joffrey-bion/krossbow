package org.hildan.krossbow.engines.mpp.frame

import org.hildan.krossbow.engines.mpp.headers.escapeForHeader

fun StompFrame.format(): String = """
$command
$formattedHeaders

${body ?: ""}
""".trimIndent()

private val StompFrame.formattedHeaders: String
    get() = headers.entries.joinToString("\n") {
        formatHeader(it.key, it.value, command.supportsHeaderEscapes)
    }

private fun formatHeader(key: String, value: String, escapeContent: Boolean): String =
    if (escapeContent) {
        "${key.escapeForHeader()}:${value.escapeForHeader()}"
    } else {
        "$key:$value"
    }

internal fun HeartBeat.formatAsHeaderValue() = "$minSendPeriodMillis,$expectedPeriodMillis"

internal fun String.toHeartBeat(): HeartBeat {
    val (minSendPeriod, expectedReceivePeriod) = split(',')
    return HeartBeat(
        minSendPeriodMillis = minSendPeriod.toInt(), expectedPeriodMillis = expectedReceivePeriod.toInt()
    )
}
