package org.hildan.krossbow.stomp.conversions.jackson

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.conversions.*
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import kotlin.test.*

class JacksonConverterTest {
    // necessary for tests on Windows to work (multiline string always uses \n as line ending)
    private val platformAgnosticIndenter = DefaultIndenter("  ", "\n")

    private val platformAgnosticPrettyPrinter = DefaultPrettyPrinter()
        .withObjectIndenter(platformAgnosticIndenter)
        .withArrayIndenter(platformAgnosticIndenter)

    private val platformAgnosticObjectMapper = jacksonObjectMapper()
        .setDefaultPrettyPrinter(platformAgnosticPrettyPrinter)

    data class Person(val name: String, val age: Int)

    private suspend fun assertSentBodyTextEquals(expectedBody: String, sendMessage: suspend (StompSession) -> Unit) =
        assertSentBodyEquals(FrameBody.Text(expectedBody), sendMessage)

    private suspend fun assertSentBodyEquals(expectedBody: FrameBody?, sendMessage: suspend (StompSession) -> Unit) =
        coroutineScope {
            val session = MockStompSession()

            launch { sendMessage(session) }

            val frame = session.waitForSentFrameAndSimulateCompletion()
            assertIs<StompFrame.Send>(frame)
            assertEquals(expectedBody, frame.body)
        }

    @Test
    fun convertAndSend_convertsToJsonString() = runTest {
        val expectedJson = "\"My message\""
        assertSentBodyTextEquals(expectedJson) { jsonSession ->
            jsonSession.withJackson().convertAndSend("/test", "My message")
        }
    }

    @Test
    fun convertAndSend_convertsToJsonObject() = runTest {
        val expectedJson = """{"name":"Bob","age":42}"""
        assertSentBodyTextEquals(expectedJson) { jsonSession ->
            jsonSession.withJackson().convertAndSend("/test", Person("Bob", 42))
        }
    }

    @Test
    fun convertAndSend_convertsToJsonArray() = runTest {
        val expectedJson = """[{"name":"Bob","age":42}]"""
        assertSentBodyTextEquals(expectedJson) { jsonSession ->
            jsonSession.withJackson().convertAndSend("/test", listOf(Person("Bob", 42)))
        }
    }

    @Test
    fun convertAndSend_convertsToJsonObject_customObjectMapper_noIndent() = runTest {
        val expectedJson = """{"name":"Bob","age":42}"""
        assertSentBodyTextEquals(expectedJson) { jsonSession ->
            jsonSession.withJackson(platformAgnosticObjectMapper)
                .convertAndSend("/test", Person("Bob", 42))
        }
    }

    @Test
    fun convertAndSend_convertsToJsonObject_customObjectMapper_indented() = runTest {
        val objectMapper = platformAgnosticObjectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT)
        val expectedJson = """
            {
              "name" : "Bob",
              "age" : 42
            }
        """.trimIndent()
        assertSentBodyTextEquals(expectedJson) { jsonSession ->
            jsonSession.withJackson(objectMapper).convertAndSend("/test", Person("Bob", 42))
        }
    }

    @Test
    fun convertAndSend_convertsNull() = runTest {
        assertSentBodyEquals(expectedBody = null) { jsonSession ->
            jsonSession.withJackson().convertAndSend<Any?>("/test", null)
        }
    }

    @Test
    fun subscribe_convertsJsonString() = runTest {
        val session = MockStompSession()
        val jsonSession = session.withJackson()

        val messages = jsonSession.subscribe<String>("/test")
        launch {
            session.simulateSubscriptionFrame(FrameBody.Text("\"message\""))
        }
        val msg = messages.first()
        assertEquals("message", msg)
    }

    @Test
    fun subscribe_convertsJsonObject() = runTest {
        val session = MockStompSession()
        val jsonSession = session.withJackson()

        val messages = jsonSession.subscribe<Person>("/test")
        launch {
            session.simulateSubscriptionFrame(FrameBody.Text("""{"name":"Bob","age":5}"""))
        }
        val msg = messages.first()
        assertEquals(Person("Bob", 5), msg)
    }

    @Test
    fun subscribe_convertsJsonArray() = runTest {
        val session = MockStompSession()
        val jsonSession = session.withJackson()

        val messages = jsonSession.subscribe<List<Person>>("/test")
        launch {
            session.simulateSubscriptionFrame(FrameBody.Text("""[{"name":"Bob","age":5}]"""))
        }
        val msg = messages.first()
        println(msg.single()::class)
        assertEquals(Person("Bob", 5), msg.single())
    }

    @Test
    fun subscribe_failsOnNullBody() = runTest {
        val session = MockStompSession()
        val jsonSession = session.withJackson()

        val messages = jsonSession.subscribe<Int>("/test")
        launch {
            session.simulateSubscriptionFrame(null)
        }

        assertFailsWith(MismatchedInputException::class) {
            messages.first()
        }
    }
}
