package org.hildan.krossbow.websocket

import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.hildan.krossbow.stomp.conversions.withJacksonConversions
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.conversions.convertAndSend
import org.hildan.krossbow.stomp.conversions.subscribe
import org.hildan.krossbow.stomp.use
import org.hildan.krossbow.test.runAsyncTest
import org.hildan.krossbow.websocket.sockjs.SockJSClient
import org.hildan.krossbow.websocket.spring.SpringDefaultWebSocketClient
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmDefaultWebSocketClientsTest {

    private val testUrl = "http://seven-wonders-online.herokuapp.com/seven-wonders-websocket"

    data class ChooseNameAction(val playerName: String)

    data class PlayerDataJackson(
        val username: String,
        val displayName: String,
        val index: Int,
        val gameOwner: Boolean,
        val user: Boolean
    )

    data class ErrorMessage(
        val message: String
    )

    @Test
    fun defaultClientTest() {
        assertEquals(SpringDefaultWebSocketClient, defaultWebSocketClient())
    }

    // Ignored for CI as long as it requires an internet connection
    // TODO spawn a local STOMP server for unit tests and interact with it
    @Ignore
    @Test
    fun basicConnect() = runAsyncTest {
        val client = StompClient(SockJSClient())
        client.connect(testUrl).withJacksonConversions().use {
            val errorsSub = subscribe<ErrorMessage>("/user/queue/errors")
            val nameSub = subscribe<PlayerDataJackson>("/user/queue/nameChoice")

            val chooseNameAction = ChooseNameAction("Bob")
            convertAndSend("/app/chooseName", chooseNameAction)

            val error = withTimeoutOrNull(100) { errorsSub.messages.receive() }
            assertNull(error)

            val response = withTimeout(2000) { nameSub.messages.receive() }
            val expected = PlayerDataJackson(
                username = "ignored",
                displayName = "Bob",
                index = -1,
                gameOwner = false,
                user = true
            )
            assertEquals(expected, response.copy(username = "ignored"))
        }
    }
}
