package org.hildan.krossbow.engines.mpp.frame

import org.hildan.krossbow.engines.mpp.headers.StompHeader
import org.hildan.krossbow.engines.mpp.headers.escapeForHeader

fun StompFrame.format(): String = """
$command
$formattedHeaders

${body ?: ""}
""".trimIndent()

private val StompFrame.formattedHeaders: String
    get() = headers.getAll().joinToString("\n") {
        // The CONNECT and CONNECTED frames do not escape the carriage return, line feed or colon octets
        // in order to remain backward compatible with STOMP 1.0
        val shouldEscape = command != StompCommands.CONNECT && command != StompCommands.CONNECTED
        it.format(shouldEscape)
    }

private fun StompHeader.format(shouldEscapeContent: Boolean): String =
    allValues.joinToString("\n") {
        formatHeader(
            key, it, shouldEscapeContent
        )
    }

private val StompHeader.allValues
    get() = listOf(value) + formerValues

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
