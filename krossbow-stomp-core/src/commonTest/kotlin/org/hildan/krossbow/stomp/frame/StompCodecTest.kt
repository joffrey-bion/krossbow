package org.hildan.krossbow.stomp.frame

import org.hildan.krossbow.stomp.headers.StompMessageHeaders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class StompCodecTest {
    private val nullChar = '\u0000'

    private val frameText1 = """
        MESSAGE
        destination:test
        message-id:123
        subscription:42
        content-length:24
        
        The body of the message.$nullChar
    """.trimIndent()

    private val frameText2WithoutBodyContentLength0 = """
        MESSAGE
        destination:test
        message-id:123
        subscription:42
        content-length:0
        
        $nullChar
    """.trimIndent()

    private val headers1 = StompMessageHeaders(
        destination = "test",
        messageId = "123",
        subscription = "42"
    ).apply {
        contentLength = 24
    }

    private val headers2 = StompMessageHeaders(
        destination = "test",
        messageId = "123",
        subscription = "42"
    ).apply {
        contentLength = 0
    }

    data class Expectation(val frameText: String, val expectedTextFrame: StompFrame, val expectedBinFrame: StompFrame)

    private val expectations = listOf(
        Expectation(
            frameText1,
            StompFrame.Message(headers1, FrameBody.Text("The body of the message.")),
            StompFrame.Message(headers1, FrameBody.Binary("The body of the message.".encodeToByteArray()))
        ),
        Expectation(
            frameText2WithoutBodyContentLength0,
            StompFrame.Message(headers2, null),
            StompFrame.Message(headers2, null)
        )
    )

    @Test
    fun decode_message_noBody_noContentLength() {
        val frameText = """
            MESSAGE
            destination:test
            message-id:123
            subscription:42
            
            $nullChar
        """.trimIndent()
        val expectedHeaders = StompMessageHeaders(
            destination = "test",
            messageId = "123",
            subscription = "42"
        )

        val expectedFrame = StompFrame.Message(expectedHeaders, body = null)
        assertEquals(expectedFrame, StompDecoder.decode(frameText))
        assertEquals(expectedFrame, StompDecoder.decode(frameText.encodeToByteArray()))
    }

    @Test
    fun decode_message_classicTextBody_noContentLength() {
        val frameText = """
            MESSAGE
            destination:test
            message-id:123
            subscription:42
            
            The body of the message.$nullChar
        """.trimIndent()
        val expectedHeaders = StompMessageHeaders(
            destination = "test",
            messageId = "123",
            subscription = "42"
        )
        val expectedBodyText = "The body of the message."

        val expectedFrame = StompFrame.Message(expectedHeaders, FrameBody.Text(expectedBodyText))
        assertEquals(expectedFrame, StompDecoder.decode(frameText))
        val expectedBinFrame = StompFrame.Message(expectedHeaders, FrameBody.Binary(expectedBodyText.encodeToByteArray()))
        assertEquals(expectedBinFrame, StompDecoder.decode(frameText.encodeToByteArray()))
    }

    @Test
    fun decodeText_message_classicTextBody_withContentLength() {
        val frameText = """
            MESSAGE
            destination:test
            message-id:123
            subscription:42
            content-length:24
            
            The body of the message.$nullChar
        """.trimIndent()
        val expectedHeaders = StompMessageHeaders(
            destination = "test",
            messageId = "123",
            subscription = "42"
        ).apply {
            contentLength = 24
        }
        val expectedBodyText = "The body of the message."

        val expectedFrame = StompFrame.Message(expectedHeaders, FrameBody.Text(expectedBodyText))
        assertEquals(expectedFrame, StompDecoder.decode(frameText))
        val expectedBinFrame = StompFrame.Message(expectedHeaders, FrameBody.Binary(expectedBodyText.encodeToByteArray()))
        assertEquals(expectedBinFrame, StompDecoder.decode(frameText.encodeToByteArray()))
    }

    @Test
    fun decodeText_message_bodyWithNullChar_withContentLength() {
        val frameText = """
            MESSAGE
            destination:test
            message-id:123
            subscription:42
            content-length:25
            
            The body of$nullChar the message.$nullChar
        """.trimIndent()
        val expectedHeaders = StompMessageHeaders(
            destination = "test",
            messageId = "123",
            subscription = "42"
        ).apply {
            contentLength = 25
        }
        val expectedBodyText = "The body of$nullChar the message."

        val expectedFrame = StompFrame.Message(expectedHeaders, FrameBody.Text(expectedBodyText))
        assertEquals(expectedFrame, StompDecoder.decode(frameText))
        val expectedBinFrame = StompFrame.Message(expectedHeaders, FrameBody.Binary(expectedBodyText.encodeToByteArray()))
        assertEquals(expectedBinFrame, StompDecoder.decode(frameText.encodeToByteArray()))
    }

    @Test
    fun decodeText_message_bodyWithNullChar_noContentLength() {
        val frameText = """
            MESSAGE
            destination:test
            message-id:123
            subscription:42
            
            The body$nullChar this should be ignored $nullChar
        """.trimIndent()
        val expectedHeaders = StompMessageHeaders(
            destination = "test",
            messageId = "123",
            subscription = "42"
        )
        val expectedBodyText = "The body"

        val expectedFrame = StompFrame.Message(expectedHeaders, FrameBody.Text(expectedBodyText))
        assertEquals(expectedFrame, StompDecoder.decode(frameText))
        val expectedBinFrame = StompFrame.Message(expectedHeaders, FrameBody.Binary(expectedBodyText.encodeToByteArray()))
        assertEquals(expectedBinFrame, StompDecoder.decode(frameText.encodeToByteArray()))
    }

    @Test
    fun testDecodeText() {
        for (e in expectations) {
            val actualFrame = StompDecoder.decode(e.frameText)
            assertEquals(e.expectedTextFrame, actualFrame)
        }
    }

    @Test
    fun testDecodeBytes() {
        for (e in expectations) {
            val actualFrame = StompDecoder.decode(e.frameText.encodeToByteArray())
            assertEquals(e.expectedBinFrame, actualFrame)
        }
    }

    @Test
    fun testEncodeToText() {
        for (e in expectations) {
            assertEquals(e.frameText, e.expectedTextFrame.encodeToText())
        }
    }

    @Test
    fun testEncodeToBytes() {
        for (e in expectations) {
            assertTrue(e.frameText.encodeToByteArray().contentEquals(e.expectedTextFrame.encodeToBytes()))
        }
    }
}
