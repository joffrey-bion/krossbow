package org.hildan.krossbow.stomp.frame

import kotlin.test.Test
import kotlin.test.assertFailsWith

class StompCodecInvalidFramesTest {
    private val nullChar = '\u0000'

    @Test
    fun decode_failsOnEmptyFrame() {
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("")
        }
    }

    @Test
    fun decode_failsOnBlankFrame() {
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("   ")
        }
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("\n")
        }
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("  \n")
        }
    }

    @Test
    fun decode_failsOnUnknownCommand() {
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("INVALID-COMMAND\nwhatever\n\n$nullChar")
        }
    }

    @Test
    fun decode_failsIfNoEmptyLineAfterCommand_noHeaders() {
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("MESSAGE\n$nullChar")
        }
    }

    @Test
    fun decode_failsIfNoEmptyLineAfterHeaders() {
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("MESSAGE\nheader1:abc$nullChar")
        }
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("MESSAGE\nheader1:abc\n$nullChar")
        }
    }

    @Test
    fun decode_failsIfNotFinishedByNullChar_withoutContentLengthHeader() {
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("MESSAGE\nheader-name:abc\n\n")
        }
    }

    @Test
    fun decode_failsIfNotFinishedByNullChar_withContentLength0() {
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("MESSAGE\ncontent-length:0\n\n")
        }
    }

    @Test
    fun decode_failsIfNotFinishedByNullChar_withPositiveContentLength() {
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("MESSAGE\ncontent-length:3\n\nabc")
        }
    }

    @Test
    fun decode_failsIfInvalidContentLength_tooSmall() {
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("MESSAGE\ncontent-length:3\n\nabcdef$nullChar")
        }
    }

    @Test
    fun decode_failsIfInvalidContentLength_tooLarge() {
        assertFailsWith<InvalidStompFrameException> {
            StompDecoder.decode("MESSAGE\ncontent-length:10\n\nabc$nullChar")
        }
    }
}
