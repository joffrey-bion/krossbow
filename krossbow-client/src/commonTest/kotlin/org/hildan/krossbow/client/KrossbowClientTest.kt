package org.hildan.krossbow.client

import kotlinx.coroutines.withTimeout
import org.hildan.krossbow.engines.useSession
import org.hildan.krossbow.testutils.runAsyncTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KrossbowClientTest {

    private val testUrl = "ws://seven-wonders-online.herokuapp.com/seven-wonders-websocket"

    data class ChooseNameAction(val playerName: String)

    data class ServerPlayerData(
        val displayName: String,
        val index: Int,
        val isGameOwner: Boolean,
        val isUser: Boolean
    )

    @Test
    fun basicConnect() = runAsyncTest {
        KrossbowClient().useSession(testUrl) {
            val (messages) = subscribe<ServerPlayerData>("/user/queue/nameChoice")

            val chooseNameAction = ChooseNameAction("Bob")
            send("/app/chooseName", chooseNameAction)

            val response = withTimeout(30000) { messages.receive() }
            val expected = ServerPlayerData(
                displayName = "Bob",
                index = -1,
                isGameOwner = false,
                isUser = false
            )
            assertEquals(expected, response.payload)
        }
    }
}
