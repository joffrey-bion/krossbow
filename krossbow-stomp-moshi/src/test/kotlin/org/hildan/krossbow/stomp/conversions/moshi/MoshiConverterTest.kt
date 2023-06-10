package org.hildan.krossbow.stomp.conversions.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.hildan.krossbow.stomp.StompSession
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
            jsonSession.withMoshi().convertAndSend("/test", "My message")
        }
    }

    @Test
    fun convertAndSend_convertsToJsonObject() = runTest {
        val expectedJson = """{"name":"Bob","age":42}"""
        assertSentBodyTextEquals(expectedJson) { jsonSession ->
            jsonSession.withMoshi(moshiKotlinReflect).convertAndSend("/test", Person("Bob", 42))
        }
    }

    @Test
    fun convertAndSend_convertsToJsonArray() = runTest {
        val expectedJson = """[{"name":"Bob","age":42}]"""
        assertSentBodyTextEquals(expectedJson) { jsonSession ->
            jsonSession.withMoshi(moshiKotlinReflect).convertAndSend("/test", listOf(Person("Bob", 42)))
        }
    }

    @Test
    fun convertAndSend_convertsNull() = runTest {
        assertSentBodyEquals(expectedBody = null) { jsonSession ->
            jsonSession.withMoshi().convertAndSend<Any?>("/test", null)
        }
    }

    @Test
    fun subscribe_convertsJsonString() = runTest {
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
    fun subscribe_convertsJsonObject() = runTest {
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
    fun subscribe_failsOnNullBody(): Unit = runTest {
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
