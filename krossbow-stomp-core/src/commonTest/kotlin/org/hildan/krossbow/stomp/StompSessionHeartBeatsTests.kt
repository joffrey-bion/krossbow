package org.hildan.krossbow.stomp

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.test.connectWithMocks
import org.hildan.krossbow.test.runAsyncTestWithTimeout
import org.hildan.krossbow.test.simulateMessageFrameReceived
import org.hildan.krossbow.test.waitForSendAndSimulateCompletion
import org.hildan.krossbow.test.waitForSubscribeAndSimulateCompletion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StompSessionHeartBeatsTests {

    @Test
    fun wsSessionClosedOnHeartBeatTimeOut() = runAsyncTestWithTimeout {
        val (wsSession, _) = connectWithMocks(
            StompConnectedHeaders(heartBeat = HeartBeat(minSendPeriodMillis = 0, expectedPeriodMillis = 300))
        )
        delay(600)
        assertTrue(wsSession.closed)
    }

    @Test
    fun receiveSubMessage_success() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks(
            StompConnectedHeaders(
                version = "1.2",
                heartBeat = HeartBeat(minSendPeriodMillis = 0, expectedPeriodMillis = 300)
            )
        )

        launch {
            val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
            wsSession.simulateMessageFrameReceived(subFrame.headers.id, "HELLO")
            wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
        }

        val sub = stompSession.subscribeText("/dest")
        val message = sub.messages.receive()
        assertEquals("HELLO", message)

        stompSession.disconnect()
    }

    @Test
    fun receiveSubMessage_failsOnHeartBeatTimeOut() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks(
            StompConnectedHeaders(heartBeat = HeartBeat(minSendPeriodMillis = 0, expectedPeriodMillis = 300))
        )

        launch {
            wsSession.waitForSubscribeAndSimulateCompletion()
        }

        val sub = stompSession.subscribeText("/dest")
        assertFailsWith(MissingHeartBeatException::class) {
            sub.messages.receive()
        }
    }

    @Test
    fun receiveSubMessage_succeedsIfKeptAlive() = runAsyncTestWithTimeout {
        val (wsSession, stompSession) = connectWithMocks(
            StompConnectedHeaders(heartBeat = HeartBeat(minSendPeriodMillis = 0, expectedPeriodMillis = 300))
        )

        launch {
            val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
            delay(100)
            wsSession.simulateTextFrameReceived("\n")
            delay(150)
            wsSession.simulateTextFrameReceived("\r\n")
            delay(150)
            wsSession.simulateMessageFrameReceived(subFrame.headers.id, "message")
        }

        val sub = stompSession.subscribeText("/dest")
        val msg = sub.messages.receive()
        assertEquals("message", msg)
    }
}
