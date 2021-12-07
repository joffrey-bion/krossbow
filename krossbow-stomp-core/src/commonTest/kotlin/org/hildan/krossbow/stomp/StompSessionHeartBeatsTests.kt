package org.hildan.krossbow.stomp

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.config.HeartBeatTolerance
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.test.connectWithMocks
import org.hildan.krossbow.test.simulateMessageFrameReceived
import org.hildan.krossbow.test.waitForSendAndSimulateCompletion
import org.hildan.krossbow.test.waitForSubscribeAndSimulateCompletion
import org.hildan.krossbow.test.waitForUnsubscribeAndSimulateCompletion
import kotlin.test.*
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

class StompSessionHeartBeatsTests {

    @Test
    fun heartBeatNegotiation_noneIfClientSaysNone() = runBlockingTest {
        val (wsSession, _) = connectWithMocks(
            StompConnectedHeaders(heartBeat = HeartBeat(minSendPeriod = 1000.milliseconds, expectedPeriod = ZERO))
        ) {
            heartBeat = HeartBeat(ZERO, ZERO)
            heartBeatTolerance = HeartBeatTolerance(incomingMargin = 100.milliseconds)
        }
        advanceTimeBy(1000L + 100 + 1)
        assertFalse(wsSession.closed, "the web socket session should NOT be closed because server heart beats should " +
                "be overruled by client config desiring no heart beats")
    }

    @Test
    fun heartBeatNegotiation_noneIfServerSaysNone() = runBlockingTest {
        val (wsSession, _) = connectWithMocks(
            StompConnectedHeaders(heartBeat = HeartBeat(minSendPeriod = 0.milliseconds, expectedPeriod = ZERO))
        ) {
            heartBeat = HeartBeat(ZERO, 1000.milliseconds)
            heartBeatTolerance = HeartBeatTolerance(incomingMargin = 100.milliseconds)
        }
        advanceTimeBy(1000L + 100 + 1)
        assertFalse(wsSession.closed, "the web socket session should NOT be closed because expected heart beats " +
                "should be overruled by the server response desiring no heart beats")
    }

    @Test
    fun heartBeatNegotiation_noneIfServerDoesntSay() = runBlockingTest {
        val (wsSession, _) = connectWithMocks(
            StompConnectedHeaders(heartBeat = null)
        ) {
            heartBeat = HeartBeat(ZERO, 1000.milliseconds)
            heartBeatTolerance = HeartBeatTolerance(incomingMargin = 100.milliseconds)
        }
        advanceTimeBy(1000L + 100 + 1)
        assertFalse(wsSession.closed, "the web socket session should NOT be closed because expected heart beats " +
                "should be overruled by the server response desiring no heart beats")
    }

    @Test
    fun heartBeatNegotiation_clientExpectsBiggestPeriod_followClient() = runBlockingTest {
        val (wsSession, _) = connectWithMocks(
            StompConnectedHeaders(heartBeat = HeartBeat(minSendPeriod = 1000.milliseconds, expectedPeriod = ZERO))
        ) {
            heartBeat = HeartBeat(ZERO, 2000.milliseconds)
            heartBeatTolerance = HeartBeatTolerance(incomingMargin = 100.milliseconds)
        }
        assertFalse(wsSession.closed, "the web socket session should NOT be closed before CLIENT heart beat timeout " +
                "of 2000ms (this is the biggest one, so it should be chosen)")
        advanceTimeBy(2000L + 100 + 1)
        assertTrue(wsSession.closed, "the web socket session should be closed if no heart beat is received in time")
    }

    @Test
    fun heartBeatNegotiation_serverExpectsBiggestPeriod_followServer() = runBlockingTest {
        val (wsSession, _) = connectWithMocks(
            StompConnectedHeaders(heartBeat = HeartBeat(minSendPeriod = 2000.milliseconds, expectedPeriod = ZERO))
        ) {
            heartBeat = HeartBeat(ZERO, 1000.milliseconds)
            heartBeatTolerance = HeartBeatTolerance(incomingMargin = 100.milliseconds)
        }
        assertFalse(wsSession.closed, "the web socket session should NOT be closed before SERVER heart beat timeout " +
                "of 2000ms (this is the biggest one, so it should be chosen)")
        advanceTimeBy(2000L + 100 + 1)
        assertTrue(wsSession.closed, "the web socket session should be closed if no heart beat is received in time")
    }

    @Test
    fun wsSessionClosedOnHeartBeatTimeOut() = runBlockingTest {
        val (wsSession, _) = connectWithMocks(
            StompConnectedHeaders(heartBeat = HeartBeat(minSendPeriod = 1000.milliseconds, expectedPeriod = ZERO))
        ) {
            heartBeat = HeartBeat(ZERO, 1000.milliseconds)
            heartBeatTolerance = HeartBeatTolerance(incomingMargin = 100.milliseconds)
        }
        assertFalse(wsSession.closed, "the web socket session should NOT be closed before heart beat timeout")
        advanceTimeBy(1000L + 100 + 1)
        assertTrue(wsSession.closed, "the web socket session should be closed if no heart beat is received in time")
    }

    @Test
    fun receiveSubMessage_success() = runBlockingTest {
        val (wsSession, stompSession) = connectWithMocks(
            StompConnectedHeaders(
                version = "1.2",
                heartBeat = HeartBeat(minSendPeriod = 1000.milliseconds, expectedPeriod = ZERO)
            )
        ) {
            heartBeat = HeartBeat(ZERO, 1000.milliseconds)
            heartBeatTolerance = HeartBeatTolerance(incomingMargin = 100.milliseconds)
        }

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
            StompConnectedHeaders(heartBeat = HeartBeat(minSendPeriod = 1000.milliseconds, expectedPeriod = ZERO))
        ) {
            heartBeat = HeartBeat(ZERO, 1000.milliseconds)
            heartBeatTolerance = HeartBeatTolerance(incomingMargin = 100.milliseconds)
        }

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
            StompConnectedHeaders(heartBeat = HeartBeat(minSendPeriod = 1000.milliseconds, expectedPeriod = ZERO))
        ) {
            heartBeat = HeartBeat(ZERO, 1000.milliseconds)
            heartBeatTolerance = HeartBeatTolerance(incomingMargin = 100.milliseconds)
        }

        launch {
            val subFrame = wsSession.waitForSubscribeAndSimulateCompletion()
            delay(800)
            wsSession.simulateTextFrameReceived("\n")
            delay(1000)
            wsSession.simulateTextFrameReceived("\r\n")
            delay(900)
            wsSession.simulateMessageFrameReceived(subFrame.headers.id, "message")
            wsSession.waitForUnsubscribeAndSimulateCompletion(subFrame.headers.id)
        }

        val messages = stompSession.subscribeText("/dest")
        val msg = messages.first()
        assertEquals("message", msg)
    }
}
