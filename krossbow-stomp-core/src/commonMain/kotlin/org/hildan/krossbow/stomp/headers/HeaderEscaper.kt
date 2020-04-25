package org.hildan.krossbow.stomp.headers

/**
 * Implementation of the header escapes described in
 * [the specification](https://stomp.github.io/stomp-specification-1.2.html#Value_Encoding).
 */
internal object HeaderEscaper {

    private const val ESCAPE_CHAR = '\\'

    fun escape(text: String): String = if (text.isEmpty()) text else buildEscapedString(text)

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

    fun unescape(text: String): String = if (text.isEmpty()) text else buildUnescapedString(text)

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
        if (escaping) {
            throw InvalidEscapeException("$ESCAPE_CHAR", "Invalid dangling escape character at end of text")
        }
    }

    private fun Char.whenEscaped(): Char = when (this) {
        'r' -> '\r'
        'n' -> '\n'
        'c' -> ':'
        '\\' -> '\\'
        else -> throw InvalidEscapeException("$ESCAPE_CHAR$this")
    }
}

class InvalidEscapeException(
    val invalidSequence: String,
    message: String = "Invalid header escape sequence '$invalidSequence'"
) : IllegalArgumentException(message)
