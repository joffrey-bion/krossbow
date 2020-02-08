package org.hildan.krossbow.websocket.test

import org.hildan.krossbow.engines.mpp.js.JsWebSocket
import org.hildan.krossbow.websocket.KWebSocketListener
import kotlin.test.Test
import kotlin.test.assertEquals

class JsWebSocketTest {

    @UseExperimental(ExperimentalStdlibApi::class)
    @Test
    fun test() = runSuspendingTest {
        val session = JsWebSocket.connect("ws://demos.kaazing.com/echo")
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
