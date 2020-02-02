package org.hildan.krossbow.websocket

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocket
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.send
import kotlinx.coroutines.withTimeout
import org.hildan.krossbow.websocket.test.runSuspendingTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorWebSocketTest {

    @UseExperimental(ExperimentalStdlibApi::class)
    @Test
    fun test_ktor_pure() = runSuspendingTest {
        val client = HttpClient { install(WebSockets) }
        client.webSocket("ws://demos.kaazing.com/echo") {
            send("hello")
            val msg = incoming.receive()
            assertEquals("hello", msg.data.decodeToString())
        }
    }

    @UseExperimental(ExperimentalStdlibApi::class)
    @Test
    fun test_ktor_explicit() = runSuspendingTest {
        val client = HttpClient { install(WebSockets) }
        client.explicitWebsocket("ws://demos.kaazing.com/echo") {
            send("hello")
            val msg = incoming.receive()
            assertEquals("hello", msg.data.decodeToString())
        }
    }

    @UseExperimental(ExperimentalStdlibApi::class)
    @Test
    fun test_ktor_simple() = runSuspendingTest {
        val client = HttpClient { install(WebSockets) }
        val session = client.simpleWebsocket("ws://demos.kaazing.com/echo")
        session.send("hello")
        val msg = session.incoming.receive()
        assertEquals("hello", msg.data.decodeToString())
        session.close()
    }

    @UseExperimental(ExperimentalStdlibApi::class)
    @Test
    fun test() = runSuspendingTest {
        val session = KtorWebSocket().connect("ws://demos.kaazing.com/echo")
        session.send("hello".encodeToByteArray())
        session.listener = object : KWebSocketListener {
            override suspend fun onBinaryMessage(bytes: ByteArray) {
                assertEquals("hello", bytes.decodeToString())
            }

            override suspend fun onTextMessage(text: String) {
                assertEquals("hello", text)
            }

            override suspend fun onError(error: Throwable) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override suspend fun onClose() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }
}
