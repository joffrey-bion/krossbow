package org.hildan.krossbow.stomp.conversions.jackson

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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

    @Test
    fun convertAndSend_convertsToJsonString() = runBlocking {
        val session = MockStompSession()
        val jsonSession = session.withJackson()

        launch { jsonSession.convertAndSend("/test", "My message") }
        val frame = session.waitForSentFrameAndSimulateCompletion()
        assertTrue(frame is StompFrame.Send)
        assertEquals("\"My message\"", frame.bodyAsText)
    }

    @Test
    fun convertAndSend_convertsToJsonObject() = runBlocking {
        val session = MockStompSession()
        val jsonSession = session.withJackson()

        launch { jsonSession.convertAndSend("/test", Person("Bob", 42)) }
        val frame = session.waitForSentFrameAndSimulateCompletion()
        assertTrue(frame is StompFrame.Send)
        assertEquals("""{"name":"Bob","age":42}""", frame.bodyAsText)
    }

    @Test
    fun convertAndSend_convertsToJsonArray() = runBlocking {
        val session = MockStompSession()
        val jsonSession = session.withJackson()

        launch { jsonSession.convertAndSend("/test", listOf(Person("Bob", 42))) }
        val frame = session.waitForSentFrameAndSimulateCompletion()
        assertTrue(frame is StompFrame.Send)
        assertEquals("""[{"name":"Bob","age":42}]""", frame.bodyAsText)
    }

    @Test
    fun convertAndSend_convertsToJsonObject_customObjectMapper_noIndent() = runBlocking {
        val session = MockStompSession()
        val jsonSession = session.withJackson(platformAgnosticObjectMapper)

        launch { jsonSession.convertAndSend("/test", Person("Bob", 42)) }
        val frame = session.waitForSentFrameAndSimulateCompletion()
        assertTrue(frame is StompFrame.Send)
        assertEquals("""{"name":"Bob","age":42}""", frame.bodyAsText)
    }

    @Test
    fun convertAndSend_convertsToJsonObject_customObjectMapper_indented() = runBlocking {
        val session = MockStompSession()
        val objectMapper = platformAgnosticObjectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT)
        val jsonSession = session.withJackson(objectMapper)

        launch { jsonSession.convertAndSend("/test", Person("Bob", 42)) }
        val frame = session.waitForSentFrameAndSimulateCompletion()
        assertTrue(frame is StompFrame.Send)
        val expectedJson = """
            {
              "name" : "Bob",
              "age" : 42
            }
        """.trimIndent()
        assertEquals(expectedJson, frame.bodyAsText)
    }

    @Test
    fun convertAndSend_convertsNull() = runBlocking {
        val session = MockStompSession()
        val jsonSession = session.withJackson()

        launch { jsonSession.convertAndSend<Any?>("/test", null) }
        val frame = session.waitForSentFrameAndSimulateCompletion()
        assertTrue(frame is StompFrame.Send)
        assertNull(frame.body)
    }

    @Test
    fun subscribe_convertsJsonString() = runBlocking {
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
    fun subscribe_convertsJsonObject() = runBlocking {
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
    fun subscribe_convertsJsonArray() = runBlocking {
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
    fun subscribe_failsOnNullBody() {
        runBlocking {
            withTimeout(1000) {
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
    }
}