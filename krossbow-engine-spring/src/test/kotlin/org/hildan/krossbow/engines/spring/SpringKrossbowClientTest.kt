package org.hildan.krossbow.engines.spring

import kotlinx.coroutines.runBlocking
import org.hildan.krossbow.engines.KrossbowClient
import kotlin.test.Test
import kotlin.test.assertEquals

class SpringKrossbowClientTest {

    @Test
    fun basicConnect() {
        runBlocking {
            val client = KrossbowClient(SpringKrossbowEngine)
            val session = client.connect("ws://seven-wonders-online.herokuapp.com/seven-wonders-websocket")
            session.subscribe<String>("/user/queue/nameChoice") {
                assertEquals("{}", it)
            }
            session.send("/app/chooseName", """{"playerName":"Bob"}""")
        }
    }
}
