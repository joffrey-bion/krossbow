package org.hildan.krossbow.stomp.conversions.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.hildan.krossbow.stomp.conversions.*
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import java.io.EOFException
import kotlin.test.*

class MoshiConverterTest {

    private val moshiKotlinReflect = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    data class Person(val name: String, val age: Int)

    @Test
    fun convertAndSend_convertsToJsonString() = runBlocking {
        val session = MockStompSession()
        val jsonSession = session.withMoshi()

        launch { jsonSession.convertAndSend("/test", "My message") }
        val frame = session.waitForSentFrameAndSimulateCompletion()
        assertTrue(frame is StompFrame.Send)
        assertEquals("\"My message\"", frame.bodyAsText)
    }

    @Test
    fun convertAndSend_convertsToJsonObject() = runBlocking {
        val session = MockStompSession()
        val jsonSession = session.withMoshi(moshiKotlinReflect)

        launch { jsonSession.convertAndSend("/test", Person("Bob", 42)) }
        val frame = session.waitForSentFrameAndSimulateCompletion()
        assertTrue(frame is StompFrame.Send)
        assertEquals("""{"name":"Bob","age":42}""", frame.bodyAsText)
    }

    @Test
    fun convertAndSend_convertsToJsonArray() = runBlocking {
        val session = MockStompSession()
        val jsonSession = session.withMoshi(moshiKotlinReflect)

        launch { jsonSession.convertAndSend("/test", listOf(Person("Bob", 42))) }
        val frame = session.waitForSentFrameAndSimulateCompletion()
        assertTrue(frame is StompFrame.Send)
        assertEquals("""[{"name":"Bob","age":42}]""", frame.bodyAsText)
    }

    @Test
    fun convertAndSend_convertsNull() = runBlocking {
        val session = MockStompSession()
        val jsonSession = session.withMoshi()

        launch { jsonSession.convertAndSend<Any?>("/test", null) }
        val frame = session.waitForSentFrameAndSimulateCompletion()
        assertTrue(frame is StompFrame.Send)
        assertNull(frame.body)
    }

    @Test
    fun subscribe_convertsJsonString() = runBlocking {
        val session = MockStompSession()
        val jsonSession = session.withMoshi()

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
        val jsonSession = session.withMoshi(moshiKotlinReflect)

        val messages = jsonSession.subscribe<Person>("/test")
        launch {
            session.simulateSubscriptionFrame(FrameBody.Text("""{"name":"Bob","age":5}"""))
        }
        val msg = messages.first()
        assertEquals(Person("Bob", 5), msg)
    }

    @Test
    fun subscribe_failsOnNullBody() {
        runBlocking {
            withTimeout(1000) {
                val session = MockStompSession()
                val jsonSession = session.withMoshi()

                val messages = jsonSession.subscribe<Int>("/test")
                launch {
                    session.simulateSubscriptionFrame(null)
                }

                assertFailsWith(EOFException::class) {
                    messages.first()
                }
            }
        }
    }
}
