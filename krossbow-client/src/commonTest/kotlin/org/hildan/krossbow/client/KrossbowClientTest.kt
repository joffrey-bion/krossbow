package org.hildan.krossbow.client

import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.hildan.krossbow.client.converters.KotlinxSerialization
import org.hildan.krossbow.testutils.runAsyncTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KrossbowClientTest {

    private val testUrl = "http://seven-wonders-online.herokuapp.com/seven-wonders-websocket"

    @Serializable
    data class ChooseNameAction(val playerName: String)

    @Serializable
    data class ServerPlayerData(
        val username: String,
        val displayName: String,
        val index: Int,
        val gameOwner: Boolean,
        val user: Boolean
    )

    @Test
    fun basicConnect() = runAsyncTest {
        val client = KrossbowClient {
            messageConverter = KotlinxSerialization.JsonConverter()
        }
        client.useSession(testUrl) {
            val (messages) = subscribe<ServerPlayerData>("/user/queue/nameChoice")

            val chooseNameAction = ChooseNameAction("Bob")
            send("/app/chooseName", chooseNameAction)

            val response = withTimeout(30000) { messages.receive() }
            val expected = ServerPlayerData(
                username = "forced",
                displayName = "Bob",
                index = -1,
                gameOwner = false,
                user = true
            )
            assertEquals(expected, response.payload.copy(username = "forced"))
        }
    }
}
