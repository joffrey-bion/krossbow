package org.hildan.krossbow.websocket.ktor

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocket
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.send
import io.ktor.util.KtorExperimentalAPI
import org.hildan.krossbow.websocket.KWebSocketListener
import org.hildan.krossbow.websocket.test.runSuspendingTest
import kotlin.test.Test
import kotlin.test.assertEquals

@UseExperimental(KtorExperimentalAPI::class, ExperimentalStdlibApi::class)
class KtorWebSocketTest {

    @Test
    fun test_ktor_pure() = runSuspendingTest {
        val client = HttpClient { install(WebSockets) }
        client.webSocket("ws://demos.kaazing.com/echo") {
            send("hello")
            val msg = incoming.receive()
            assertEquals("hello", msg.data.decodeToString())
        }
    }

    @Test
    fun test_ktor_explicit() = runSuspendingTest {
        val client = HttpClient { install(WebSockets) }
        client.explicitWebsocket("ws://demos.kaazing.com/echo") {
            send("hello")
            val msg = incoming.receive()
            assertEquals("hello", msg.data.decodeToString())
        }
    }

    @Test
    fun test_ktor_simple() = runSuspendingTest {
        val client = HttpClient { install(WebSockets) }
        val session = client.simpleWebsocket("ws://demos.kaazing.com/echo")
        session.send("hello")
        val msg = session.incoming.receive()
        assertEquals("hello", msg.data.decodeToString())
        session.close()
    }

    @Test
    fun test() = runSuspendingTest {
        val session = KtorWebSocket().connect("ws://demos.kaazing.com/echo")
        session.listener = object : KWebSocketListener {
            override suspend fun onBinaryMessage(bytes: ByteArray) {
                assertEquals("hello", bytes.decodeToString())
            }

            override suspend fun onTextMessage(text: String) {
                assertEquals("hello", text)
            }

            override suspend fun onError(error: Throwable) {
                TODO("not implemented")
            }

            override suspend fun onClose() {
                TODO("not implemented")
            }
        }
        session.sendBinary("hello".encodeToByteArray())
    }
}
