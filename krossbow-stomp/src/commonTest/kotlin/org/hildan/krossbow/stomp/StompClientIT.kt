package org.hildan.krossbow.stomp

import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import org.hildan.krossbow.stomp.conversions.kxserialization.convertAndSend
import org.hildan.krossbow.stomp.conversions.kxserialization.subscribe
import org.hildan.krossbow.stomp.conversions.kxserialization.withJsonConversions
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
    @OptIn(ImplicitReflectionSerializer::class)
    @Ignore
    @Test
    fun basicConnect() = runAsyncTestWithTimeout(60000) {
        val client = StompClient(SockJSClient()) {
            connectionTimeoutMillis = 40000
        }
        client.connect(testUrl).withJsonConversions().use {
            val nameSub = subscribe<ServerPlayerData>("/user/queue/nameChoice")

            val chooseNameAction = ChooseNameAction("Bob")
            convertAndSend("/app/chooseName", chooseNameAction)

            val response = withTimeout(30000) { nameSub.messages.receive() }
            val expected = ServerPlayerData(
                username = "forced",
                displayName = "Bob",
                index = -1,
                gameOwner = false,
                user = true
            )
            assertEquals(expected, response.body.copy(username = "forced"))
        }
    }
}
