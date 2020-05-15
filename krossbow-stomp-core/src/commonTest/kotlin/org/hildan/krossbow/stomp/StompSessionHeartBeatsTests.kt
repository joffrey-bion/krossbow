package org.hildan.krossbow.stomp

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.test.connectWithMocks
import org.hildan.krossbow.test.simulateMessageFrameReceived
import org.hildan.krossbow.test.waitForSendAndSimulateCompletion
import org.hildan.krossbow.test.waitForSubscribeAndSimulateCompletion
import org.hildan.krossbow.test.waitForUnsubscribeAndSimulateCompletion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StompSessionHeartBeatsTests {

    @Test
    fun wsSessionClosedOnHeartBeatTimeOut() = runBlockingTest {
        val (wsSession, _) = connectWithMocks(
            StompConnectedHeaders(heartBeat = HeartBeat(minSendPeriodMillis = 0, expectedPeriodMillis = 300))
        )
        delay(600)
        assertTrue(wsSession.closed)
    }

    @Test
    fun receiveSubMessage_success() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks(
            StompConnectedHeaders(
                version = "1.2",
                heartBeat = HeartBeat(minSendPeriodMillis = 0, expectedPeriodMillis = 500)
            )
        )

        launch {
            val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
            wsSession.simulateMessageFrameReceived(subFrame.headers.id, "HELLO")
            wsSession.waitForSendAndSimulateCompletion(StompCommand.UNSUBSCRIBE)
            wsSession.waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
            wsSession.expectClose()
        }

        val messages = stompSession.subscribeText("/dest")
        val msg = messages.first()
        assertEquals("HELLO", msg)

        stompSession.disconnect()
    }

    @Test
    fun receiveSubMessage_failsOnHeartBeatTimeOut() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks(
            StompConnectedHeaders(heartBeat = HeartBeat(minSendPeriodMillis = 0, expectedPeriodMillis = 300))
        )

        launch {
            wsSession.waitForSubscribeAndSimulateCompletion()
            wsSession.expectClose()
        }

        val messages = stompSession.subscribeText("/dest")
        assertFailsWith(MissingHeartBeatException::class) {
            messages.first()
        }
    }

    @Test
    fun receiveSubMessage_succeedsIfKeptAlive() = runBlockingTest {
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
            wsSession.waitForUnsubscribeAndSimulateCompletion(subFrame.headers.id)
        }

        val messages = stompSession.subscribeText("/dest")
        val msg = messages.first()
        assertEquals("message", msg)
    }
}
