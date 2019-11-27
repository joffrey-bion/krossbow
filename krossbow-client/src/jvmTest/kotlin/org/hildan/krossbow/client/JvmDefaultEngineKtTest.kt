package org.hildan.krossbow.client

import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.hildan.krossbow.client.converters.JacksonConverter
import org.hildan.krossbow.engines.spring.SpringKrossbowEngine
import org.hildan.krossbow.testutils.runAsyncTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmDefaultEngineKtTest {

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
    fun defaultEngineTest() {
        assertEquals(SpringKrossbowEngine, defaultEngine())
    }

    @Test
    fun basicConnect() = runAsyncTest {
        val client = KrossbowClient {
            messageConverter = JacksonConverter()
        }
        client.useSession(testUrl) {
            val (errors) = subscribe<ErrorMessage>("/user/queue/errors")
            val (messages) = subscribe<PlayerDataJackson>("/user/queue/nameChoice")

            val chooseNameAction = ChooseNameAction("Bob")
            send("/app/chooseName", chooseNameAction)

            val error = withTimeoutOrNull(100) { errors.receive() }
            assertNull(error)

            val response = withTimeout(2000) { messages.receive() }
            val expected = PlayerDataJackson(
                username = "ignored",
                displayName = "Bob",
                index = -1,
                gameOwner = false,
                user = true
            )
            assertEquals(expected, response.payload.copy(username = "ignored"))
        }
    }
}
