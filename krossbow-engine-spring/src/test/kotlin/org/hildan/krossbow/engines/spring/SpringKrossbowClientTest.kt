package org.hildan.krossbow.engines.spring

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.hildan.krossbow.engines.KrossbowClient
import org.hildan.krossbow.engines.KrossbowConfig
import org.hildan.krossbow.engines.subscribe
import org.hildan.krossbow.engines.useSession
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandler
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import java.lang.Exception
import java.lang.reflect.Type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SpringKrossbowClientTest {

    private val testUrl = "ws://localhost:8080/seven-wonders-websocket"

    @Test
    fun basicConnect() {
        runBlocking {
            KrossbowClient(SpringKrossbowEngine).useSession(testUrl) {
                val (messages) = subscribe<ByteArray>("/user/queue/nameChoice")
//                val (errors) = subscribe<ByteArray>("/user/queue/errors")

                val chooseNameAction = """{"playerName":"Bob"}"""
                send("/app/chooseName", chooseNameAction.toByteArray())

                try {
                    val response = withTimeoutOrNull(500) { messages.receive() }
                } catch (e: Exception) {
                    println("Yay")
                }
//                val error = withTimeoutOrNull(100) { errors.receive() }
//                assertNotNull(response)
//                assertNull(error)
            }
        }
    }
}
