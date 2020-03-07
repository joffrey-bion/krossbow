package org.hildan.krossbow.stomp.frame

import org.hildan.krossbow.stomp.headers.StompMessageHeaders
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalStdlibApi::class)
class StompDecoderTest {
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
    fun testParseText() {
        for (e in expectations) {
            val actualFrame = StompDecoder.decode(e.frameText)
            assertEquals(e.expectedTextFrame, actualFrame)
        }
    }

    @Test
    fun testParseBytes() {
        for (e in expectations) {
            val actualFrame = StompDecoder.decode(e.frameText.encodeToByteArray())
            assertEquals(e.expectedBinFrame, actualFrame)
        }
    }
}
