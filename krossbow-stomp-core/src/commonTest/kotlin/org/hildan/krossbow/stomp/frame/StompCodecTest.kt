package org.hildan.krossbow.stomp.frame

import kotlinx.io.bytestring.*
import org.hildan.krossbow.stomp.headers.*
import kotlin.test.*

class StompCodecTest {
    private val nullChar = '\u0000'

    @Test
    fun stomp_basic() {
        val frameText = """
            STOMP
            host:some.host
            accept-version:1.2
            
            $nullChar
        """.trimIndent()

        val headers = StompConnectHeaders(host = "some.host") {
            acceptVersion = listOf("1.2")
        }
        val frame = StompFrame.Stomp(headers)
        assertEncodingDecoding(frameText, frame, frame)
    }

    @Test
    fun connect_basic() {
        val frameText = """
            CONNECT
            host:some.host
            accept-version:1.2
            
            $nullChar
        """.trimIndent()

        val headers = StompConnectHeaders(host = "some.host") {
            acceptVersion = listOf("1.2")
        }
        val frame = StompFrame.Connect(headers)
        assertEncodingDecoding(frameText, frame, frame)
    }

    @Test
    fun connect_default() {
        val frameText = """
            CONNECT
            host:some.host
            accept-version:1.2,1.1,1.0
            
            $nullChar
        """.trimIndent()

        val headers = StompConnectHeaders(host = "some.host")
        val frame = StompFrame.Connect(headers)
        assertEncodingDecoding(frameText, frame, frame)
    }

    @Test
    fun connect_credentials() {
        val frameText = """
            CONNECT
            host:some.host
            accept-version:1.2
            login:bob
            passcode:mypass
            
            $nullChar
        """.trimIndent()

        val headers = StompConnectHeaders(host = "some.host") {
            acceptVersion = listOf("1.2")
            login = "bob"
            passcode = "mypass"
        }
        val frame = StompFrame.Connect(headers)
        assertEncodingDecoding(frameText, frame, frame)
    }

    @Test
    fun connect_custom_headers() {
        val frameText = """
            CONNECT
            host:some.host
            accept-version:1.2
            login:bob
            passcode:mypass
            Authorization:Bearer -jwt-
            
            $nullChar
        """.trimIndent()

        val headers = StompConnectHeaders(host = "some.host") {
            acceptVersion = listOf("1.2")
            login = "bob"
            passcode = "mypass"
            this["Authorization"] = "Bearer -jwt-"
        }
        val frame = StompFrame.Connect(headers)
        assertEncodingDecoding(frameText, frame, frame)
    }

    @Test
    fun connect_versions() {
        val frameText = """
            CONNECT
            host:some.host
            accept-version:1.0,1.1,1.2
            
            $nullChar
        """.trimIndent()

        val headers = StompConnectHeaders(host = "some.host") {
            acceptVersion = listOf("1.0", "1.1", "1.2")
        }
        val frame = StompFrame.Connect(headers)
        assertEncodingDecoding(frameText, frame, frame)
    }

    @Test
    fun connect_without_host() {
        val frameText = """
            CONNECT
            accept-version:1.2
            
            $nullChar
        """.trimIndent()

        val headers = StompConnectHeaders(host = null) {
            acceptVersion = listOf("1.2")
        }
        val frame = StompFrame.Connect(headers)
        assertEncodingDecoding(frameText, frame, frame)
    }

    @Test
    fun connected() {
        val frameText = """
            CONNECTED
            version:1.2
            
            $nullChar
        """.trimIndent()

        val headers = StompConnectedHeaders { version = "1.2" }
        val frame = StompFrame.Connected(headers)
        assertEncodingDecoding(frameText, frame, frame)
    }

    @Test
    fun connected_noVersionShouldBe10() {
        val frameText = """
            CONNECTED
            
            $nullChar
        """.trimIndent()

        val actualFrame = frameText.decodeToStompFrame()
        assertIs<StompFrame.Connected>(actualFrame)
        assertEquals("1.0", actualFrame.headers.version)
    }

    @Test
    fun subscribe_custom_headers() {
        val frameText = """
            SUBSCRIBE
            destination:/topic/dest
            id:0
            ack:auto
            receipt:message-1234
            Authorization:Bearer -jwt-
            
            $nullChar
        """.trimIndent()

        val headers = StompSubscribeHeaders(destination = "/topic/dest") {
            id = "0"
            ack = AckMode.AUTO
            receipt = "message-1234"
            this["Authorization"] = "Bearer -jwt-"
        }
        val frame = StompFrame.Subscribe(headers)
        assertEncodingDecoding(frameText, frame, frame)
    }

    @Test
    fun send_noBody_noContentLength() {
        val frameText = """
            SEND
            destination:test
            
            $nullChar
        """.trimIndent()

        val headers = StompSendHeaders(destination = "test")
        val frame = StompFrame.Send(headers, body = null)
        assertEncodingDecoding(frameText, frame, frame)
    }

    @Test
    fun send_classicTextBody_noContentLength() {
        val frameText = """
            SEND
            destination:test
            
            The body of the message.$nullChar
        """.trimIndent()

        val headers = StompSendHeaders(destination = "test")
        val bodyText = "The body of the message."
        val textFrame = StompFrame.Send(headers, FrameBody.Text(bodyText))
        val binFrame = StompFrame.Send(headers, FrameBody.Binary(bodyText.encodeToByteString()))
        assertEncodingDecoding(frameText, textFrame, binFrame)
    }

    @Test
    fun send_classicTextBody_withContentLength() {
        val frameText = """
            SEND
            destination:test
            content-length:24
            
            The body of the message.$nullChar
        """.trimIndent()

        val headers = StompSendHeaders(destination = "test") { contentLength = 24 }
        val bodyText = "The body of the message."
        val textFrame = StompFrame.Send(headers, FrameBody.Text(bodyText))
        val binFrame = StompFrame.Send(headers, FrameBody.Binary(bodyText.encodeToByteString()))
        assertEncodingDecoding(frameText, textFrame, binFrame)
    }

    @Test
    fun send_bodyWithNullChar_withContentLength() {
        val frameText = """
            SEND
            destination:test
            content-length:25
            
            The body of$nullChar the message.$nullChar
        """.trimIndent()

        val headers = StompSendHeaders(destination = "test") { contentLength = 25 }
        val bodyText = "The body of$nullChar the message."
        val textFrame = StompFrame.Send(headers, FrameBody.Text(bodyText))
        val binFrame = StompFrame.Send(headers, FrameBody.Binary(bodyText.encodeToByteString()))
        assertEncodingDecoding(frameText, textFrame, binFrame)
    }

    @Test
    fun send_classicTextBody_contentLengthTooShort() {
        val headers = StompSendHeaders(destination = "test") {
            contentLength = 5
        }
        val bodyText = "The body of the message."
        val textFrame = StompFrame.Send(headers, FrameBody.Text(bodyText))
        val binFrame = StompFrame.Send(headers, FrameBody.Binary(bodyText.encodeToByteString()))
        assertFailsWith<InvalidContentLengthException> { textFrame.encodeToText() }
        assertFailsWith<InvalidContentLengthException> { binFrame.encodeToByteString() }
    }

    @Test
    fun message_noBody_noContentLength() {
        val frameText = """
            MESSAGE
            destination:test
            message-id:123
            subscription:42
            
            $nullChar
        """.trimIndent()

        val headers = StompMessageHeaders(
            destination = "test",
            messageId = "123",
            subscription = "42"
        )
        val frame = StompFrame.Message(headers, body = null)
        assertEncodingDecoding(frameText, frame, frame)
    }

    @Test
    fun message_classicTextBody_noContentLength() {
        val frameText = """
            MESSAGE
            destination:test
            message-id:123
            subscription:42
            
            The body of the message.$nullChar
        """.trimIndent()

        val headers = StompMessageHeaders(
            destination = "test",
            messageId = "123",
            subscription = "42"
        )
        val bodyText = "The body of the message."
        val textFrame = StompFrame.Message(headers, FrameBody.Text(bodyText))
        val binFrame = StompFrame.Message(headers, FrameBody.Binary(bodyText.encodeToByteString()))
        assertEncodingDecoding(frameText, textFrame, binFrame)
    }

    @Test
    fun message_classicTextBody_withContentLength() {
        val frameText = """
            MESSAGE
            destination:test
            message-id:123
            subscription:42
            content-length:24
            
            The body of the message.$nullChar
        """.trimIndent()

        val headers = StompMessageHeaders(
            destination = "test",
            messageId = "123",
            subscription = "42"
        ) {
            contentLength = 24
        }
        val bodyText = "The body of the message."
        val textFrame = StompFrame.Message(headers, FrameBody.Text(bodyText))
        val binFrame = StompFrame.Message(headers, FrameBody.Binary(bodyText.encodeToByteString()))
        assertEncodingDecoding(frameText, textFrame, binFrame)
    }

    @Test
    fun message_bodyWithNullChar_withContentLength() {
        val frameText = """
            MESSAGE
            destination:test
            message-id:123
            subscription:42
            content-length:25
            
            The body of$nullChar the message.$nullChar
        """.trimIndent()

        val headers = StompMessageHeaders(
            destination = "test",
            messageId = "123",
            subscription = "42"
        ) {
            contentLength = 25
        }
        val bodyText = "The body of$nullChar the message."
        val textFrame = StompFrame.Message(headers, FrameBody.Text(bodyText))
        val binFrame = StompFrame.Message(headers, FrameBody.Binary(bodyText.encodeToByteString()))
        assertEncodingDecoding(frameText, textFrame, binFrame)
    }

    @Test
    fun message_bodyUtf8_withContentLength() {
        val frameText = """
            MESSAGE
            destination:/topic/dest
            message-id:456
            subscription:123
            content-length:2
            
            é$nullChar
        """.trimIndent()

        val headers = StompMessageHeaders(
            destination = "/topic/dest",
            messageId = "456",
            subscription = "123",
        ) {
            contentLength = 2
        }
        val bodyText = "é"
        val textFrame = StompFrame.Message(headers, FrameBody.Text(bodyText))
        val binFrame = StompFrame.Message(headers, FrameBody.Binary(bodyText.encodeToByteString()))
        assertEncodingDecoding(frameText, textFrame, binFrame)
    }

    @Test
    fun message_bodyUtf8Json_withContentLength() {
        val frameText = """
            MESSAGE
            destination:/topic/dest
            message-id:456
            subscription:123
            content-length:13
            content-type:application/json
            
            {"test":"é"}$nullChar
        """.trimIndent()

        val headers = StompMessageHeaders(
            destination = "/topic/dest",
            messageId = "456",
            subscription = "123",
        ) {
            contentLength = 13
            contentType = "application/json"
        }
        val bodyText = """{"test":"é"}"""
        val textFrame = StompFrame.Message(headers, FrameBody.Text(bodyText))
        val binFrame = StompFrame.Message(headers, FrameBody.Binary(bodyText.encodeToByteString()))
        assertEncodingDecoding(frameText, textFrame, binFrame)
    }

    @Test
    fun message_classicTextBody_contentLengthTooShort() {
        val headers = StompMessageHeaders(
            destination = "test",
            messageId = "123",
            subscription = "42"
        ) {
            contentLength = 5
        }
        val bodyText = "The body of the message."
        val textFrame = StompFrame.Message(headers, FrameBody.Text(bodyText))
        val binFrame = StompFrame.Message(headers, FrameBody.Binary(bodyText.encodeToByteString()))
        assertFailsWith<InvalidContentLengthException> { textFrame.encodeToText() }
        assertFailsWith<InvalidContentLengthException> { binFrame.encodeToByteString() }
    }

    private fun assertEncodingDecoding(frameText: String, textFrame: StompFrame, binFrame: StompFrame) {
        assertEquals(textFrame, frameText.decodeToStompFrame())
        assertEquals(binFrame, frameText.encodeToByteString().decodeToStompFrame())
        assertEquals(frameText, textFrame.encodeToText())
        assertEquals(frameText.encodeToByteString(), textFrame.encodeToByteString())
    }
}
