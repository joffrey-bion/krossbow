package org.hildan.krossbow.websocket

import kotlin.test.*

private const val CHAR_4_BYTES = "\uD83D\uDCA3" // bomb emoji 💣

class WebSocketUtilsTest {

    @Test
    fun truncateUtf8BytesLengthTo_noTruncationIfSmallerThanLimit() {
        assertEquals("", "".truncateUtf8BytesLengthTo(3))
        assertEquals("1234", "1234".truncateUtf8BytesLengthTo(10))
    }

    @Test
    fun truncateUtf8BytesLengthTo_noTruncationIfSmallerThanLimit_multiByteChar() {
        assertEquals("1234$CHAR_4_BYTES", "1234$CHAR_4_BYTES".truncateUtf8BytesLengthTo(10))
    }

    @Test
    fun truncateUtf8BytesLengthTo_noTruncationIfSameLengthAsLimit() {
        assertEquals("", "".truncateUtf8BytesLengthTo(0))
        assertEquals("1234", "1234".truncateUtf8BytesLengthTo(4))
    }

    @Test
    fun truncateUtf8BytesLengthTo_noTruncationIfSameLengthAsLimit_multiByteChar() {
        assertEquals("1234$CHAR_4_BYTES", "1234$CHAR_4_BYTES".truncateUtf8BytesLengthTo(8))
    }

    @Test
    fun truncateUtf8BytesLengthTo_simpleTruncation() {
        assertEquals("1234", "123456789".truncateUtf8BytesLengthTo(4))
    }

    @Test
    fun truncateUtf8BytesLengthTo_simpleTruncation_multiByteChar() {
        assertEquals("1234", "1234$CHAR_4_BYTES".truncateUtf8BytesLengthTo(4))
        assertEquals(CHAR_4_BYTES, "${CHAR_4_BYTES}1234".truncateUtf8BytesLengthTo(4))
    }

    @Test
    fun truncateUtf8BytesLengthTo_truncateInTheMiddleOfMultiByteChar() {
        assertEquals("1234", "1234$CHAR_4_BYTES".truncateUtf8BytesLengthTo(5))
        assertEquals("1234", "1234$CHAR_4_BYTES".truncateUtf8BytesLengthTo(6))
        assertEquals("1234", "1234$CHAR_4_BYTES".truncateUtf8BytesLengthTo(7))
    }
}
