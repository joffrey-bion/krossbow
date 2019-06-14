package org.hildan.krossbow.engines.spring

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.hildan.krossbow.engines.KrossbowClient
import org.hildan.krossbow.engines.useSession
import kotlin.test.Test
import kotlin.test.assertEquals

class SpringKrossbowClientTest {

    private val testUrl = "ws://localhost:8080/seven-wonders-websocket"

    data class ServerPlayerData(
        val displayName: String,
        val index: Int,
        val isGameOwner: Boolean,
        val isUser: Boolean
    )

    @Test
    fun basicConnect() {
        runBlocking {
            KrossbowClient(SpringKrossbowEngine).useSession(testUrl) {
                val (messages) = subscribe<ServerPlayerData>("/user/queue/nameChoice")

                val chooseNameAction = """{"playerName":"Bob"}"""
                send("/app/chooseName", chooseNameAction.toByteArray())

                val response = withTimeout(500) { messages.receive() }
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
}
