package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.hildan.krossbow.websocket.KWebSocketClient
import org.hildan.krossbow.websocket.KWebSocketListener
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

expect fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit)

@UseExperimental(ExperimentalStdlibApi::class)
fun testKaazingEchoWs(websocketClient: KWebSocketClient, protocol: String) = runSuspendingTest {
    val session = withTimeoutOrNull(1000) {
        websocketClient.connect("$protocol://demos.kaazing.com/echo")
    }
    assertNotNull(session, "connection timeout")
    val messageChannel = Channel<String>()
    session.listener = object : KWebSocketListener {
        override suspend fun onBinaryMessage(bytes: ByteArray) {
            messageChannel.send(bytes.decodeToString())
        }
        override suspend fun onTextMessage(text: String) {
            messageChannel.send(text)
        }
        override suspend fun onError(error: Throwable) {
            throw RuntimeException("onError:", error)
        }
        override suspend fun onClose() {}
    }
    session.sendBinary("hello".encodeToByteArray())
    val message = withTimeout(1000) { messageChannel.receive() }
    assertEquals("hello", message)
    session.close()
}
