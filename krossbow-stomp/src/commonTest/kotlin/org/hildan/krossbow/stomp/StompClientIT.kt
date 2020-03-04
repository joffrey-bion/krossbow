package org.hildan.krossbow.stomp

import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.hildan.krossbow.converters.KotlinxSerialization
import org.hildan.krossbow.test.runAsyncTestWithTimeout
import org.hildan.krossbow.websocket.sockjs.SockJSClient
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class StompClientIT {

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

    // Ignored for CI as long as it requires an internet connection
    // TODO spawn a local STOMP server for unit tests and interact with it
    @Ignore
    @Test
    fun basicConnect() = runAsyncTestWithTimeout(60000) {
        val client = StompClient(SockJSClient()) {
            messageConverter = KotlinxSerialization.JsonConverter()
            connectionTimeoutMillis = 40000
        }
        client.useSession(testUrl) {
            val nameSub = subscribe<ServerPlayerData>("/user/queue/nameChoice")

            val chooseNameAction = ChooseNameAction("Bob")
            send("/app/chooseName", chooseNameAction)

            val response = withTimeout(30000) { nameSub.messages.receive() }
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
