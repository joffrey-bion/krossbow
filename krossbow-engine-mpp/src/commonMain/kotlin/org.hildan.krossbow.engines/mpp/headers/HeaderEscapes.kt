package org.hildan.krossbow.engines.mpp.headers

private const val ESCAPE_CHAR = '\\'

internal fun String.escapeForHeader(): String = if (isEmpty()) this else buildEscapedString(
    this
)

private fun buildEscapedString(str: String): String = buildString(str.length) {
    for (c in str) {
        when (c) {
            '\r' -> append("""\r""")
            '\n' -> append("""\n""")
            ':' -> append("""\c""")
            '\\' -> append("""\\""")
            else -> append(c)
        }
    }
}

internal fun String.unescapeHeader(): String = if (isEmpty()) this else buildUnescapedString(
    this
)

private fun buildUnescapedString(escapedStr: String): String = buildString(escapedStr.length) {
    var escaping = false
    for (c in escapedStr) {
        if (escaping) {
            append(c.whenEscaped())
            escaping = false
        } else {
            when (c) {
                ESCAPE_CHAR -> escaping = true
                else -> append(c)
            }
        }
    }
}

private fun Char.whenEscaped(): Char = when (this) {
    'r' -> '\r'
    'n' -> '\n'
    'c' -> ':'
    '\\' -> '\\'
    else -> throw InvalidEscapeException("\\$this")
}

class InvalidEscapeException(val escapeSequence: String) : IllegalArgumentException(escapeSequence)
