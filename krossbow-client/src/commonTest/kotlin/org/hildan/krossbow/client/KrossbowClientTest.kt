package org.hildan.krossbow.client

import org.hildan.krossbow.testutils.runAsyncTest
import kotlin.test.Test

class KrossbowClientTest {

    @Test
    fun basicConnect() = runAsyncTest {
        val client = KrossbowClient()
        client.connect("ws://seven-wonders-online.herokuapp.com/seven-wonders-websocket")
    }
}
