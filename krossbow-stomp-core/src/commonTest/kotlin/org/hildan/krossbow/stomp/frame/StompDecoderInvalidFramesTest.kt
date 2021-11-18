package org.hildan.krossbow.stomp.frame

import kotlin.test.Test
import kotlin.test.assertFailsWith

class StompDecoderInvalidFramesTest {
    private val nullChar = '\u0000'

    @Test
    fun decode_failsOnEmptyFrame() {
        assertInvalidFrame("")
    }

    @Test
    fun decode_failsOnBlankFrame() {
        assertInvalidFrame("   ")
        assertInvalidFrame("\n")
        assertInvalidFrame("  \n")
    }

    @Test
    fun decode_failsOnUnknownCommand() {
        assertInvalidFrame("INVALID-COMMAND\nwhatever\n\n$nullChar")
    }

    @Test
    fun decode_failsIfNoEmptyLineAfterCommand_noHeaders() {
        assertInvalidFrame("MESSAGE\n$nullChar")
    }

    @Test
    fun decode_failsIfNoEmptyLineAfterHeaders() {
        assertInvalidFrame("MESSAGE\nheader1:abc$nullChar")
        assertInvalidFrame("MESSAGE\nheader1:abc\n$nullChar")
    }

    @Test
    fun decode_failsIfNotFinishedByNullChar_withoutContentLengthHeader() {
        assertInvalidFrame("MESSAGE\nheader-name:abc\n\n")
    }

    @Test
    fun decode_failsIfNotFinishedByNullChar_withContentLength0() {
        assertInvalidFrame("MESSAGE\ncontent-length:0\n\n")
    }

    @Test
    fun decode_failsIfNotFinishedByNullChar_withPositiveContentLength() {
        assertInvalidFrame("MESSAGE\ncontent-length:3\n\nabc")
    }

    @Test
    fun decode_failsIfInvalidContentLength_tooSmall() {
        assertInvalidFrame("MESSAGE\ncontent-length:3\n\nabcdef$nullChar")
    }

    @Test
    fun decode_failsIfInvalidContentLength_tooLarge() {
        assertInvalidFrame("MESSAGE\ncontent-length:10\n\nabc$nullChar")
    }

    @Test
    fun decode_failsIfNonEolCharactersAfterEndOfFrame() {
        assertInvalidFrame("MESSAGE\n\nabc${nullChar}invalid")
        assertInvalidFrame("MESSAGE\n\nabc$nullChar\n\r\ninvalid")
    }

    private fun assertInvalidFrame(frameText: String) {
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode(frameText)
        }
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode(frameText.encodeToByteArray())
        }
    }
}
