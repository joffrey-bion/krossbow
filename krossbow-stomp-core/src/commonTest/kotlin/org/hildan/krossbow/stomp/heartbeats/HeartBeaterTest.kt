package org.hildan.krossbow.stomp.heartbeats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.config.HeartBeatTolerance
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val TEST_PERIOD_CONFIG = 5.seconds
private val TEST_OUTGOING_MARGIN = 200.milliseconds
private val TEST_INCOMING_MARGIN = 500.milliseconds

private val TEST_SEND_PERIOD = TEST_PERIOD_CONFIG - TEST_OUTGOING_MARGIN
private val TEST_SEND_PERIOD_MILLIS = TEST_SEND_PERIOD.inWholeMilliseconds

private val TEST_RECEIVED_PERIOD = TEST_PERIOD_CONFIG + TEST_INCOMING_MARGIN
private val TEST_RECEIVED_PERIOD_MILLIS = TEST_RECEIVED_PERIOD.inWholeMilliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class HeartBeaterTest {

    private class HeartBeaterConsumer(
        heartBeat: HeartBeat,
        tolerance: HeartBeatTolerance = HeartBeatTolerance(TEST_OUTGOING_MARGIN, TEST_INCOMING_MARGIN),
    ) {
        val sent = Channel<Unit>()
        val received = Channel<Unit>()

        val heartBeater = HeartBeater(
            heartBeat = heartBeat,
            tolerance = tolerance,
            sendHeartBeat = { sent.send(Unit) },
            onMissingHeartBeat = { received.send(Unit) }
        )
    }

    @Test
    fun zeroSendAndReceive_nothingHappens() = runTest {
        val hbc = HeartBeaterConsumer(HeartBeat(ZERO, ZERO))
        assertTrue(hbc.sent.isEmpty, "shouldn't do anything before starting")
        assertTrue(hbc.received.isEmpty, "shouldn't do anything before starting")
        hbc.heartBeater.startIn(this)
        advanceTimeBy(10000)
        assertTrue(hbc.sent.isEmpty, "shouldn't do anything with heart-beat 0,0")
        assertTrue(hbc.received.isEmpty, "shouldn't do anything with heart-beat 0,0")
    }

    @Test
    fun zeroSendAndReceive_canCallNotifyWithoutEffect() = runTest {
        val hbc = HeartBeaterConsumer(HeartBeat(ZERO, ZERO))
        hbc.heartBeater.startIn(this)
        hbc.heartBeater.notifyMsgReceived()
        hbc.heartBeater.notifyMsgSent()
    }

    @Test
    fun nonZeroSend_zeroReceive_sendsHeartBeats() = runTest {
        val hbc = HeartBeaterConsumer(HeartBeat(TEST_PERIOD_CONFIG, ZERO))
        assertTrue(hbc.sent.isEmpty, "shouldn't do anything if not started")

        val job = hbc.heartBeater.startIn(this)
        assertTrue(hbc.sent.isEmpty, "should NOT have sent a heartbeat right away")

        advanceTimeBy(TEST_SEND_PERIOD_MILLIS)
        assertTrue(hbc.sent.isEmpty, "should NOT have sent a heartbeat before 1st period of inactivity")

        runCurrent()
        assertTrue(hbc.sent.tryReceive().isSuccess, "should have sent a heartbeat after 1st period of inactivity")

        advanceTimeBy(TEST_SEND_PERIOD_MILLIS)
        runCurrent()
        assertTrue(hbc.sent.tryReceive().isSuccess, "should have sent a heartbeat after 2nd period of inactivity")

        repeat(3) {
            hbc.heartBeater.notifyMsgSent()
            advanceTimeBy(TEST_SEND_PERIOD_MILLIS / 2)
        }
        runCurrent()
        assertTrue(hbc.sent.isEmpty, "shouldn't have sent any heart beat since messages were sent")

        hbc.heartBeater.notifyMsgSent()
        advanceTimeBy(TEST_SEND_PERIOD_MILLIS)
        runCurrent()
        assertTrue(hbc.sent.tryReceive().isSuccess, "should have sent a heartbeat after 3rd period of inactivity")

        assertTrue(hbc.received.isEmpty, "shouldn't have received any heart beat")
        job.cancel()
    }

    @Test
    fun nonZeroSend_zeroReceive_canCallNotifyReceived() = runTest {
        val hbc = HeartBeaterConsumer(HeartBeat(TEST_PERIOD_CONFIG, ZERO))
        val job = hbc.heartBeater.startIn(this)
        hbc.heartBeater.notifyMsgReceived()
        job.cancel()
    }

    @Test
    fun zeroSend_nonZeroReceive_sends() = runTest {
        val hbc = HeartBeaterConsumer(HeartBeat(ZERO, TEST_PERIOD_CONFIG))
        assertTrue(hbc.received.isEmpty, "shouldn't do anything if not started")

        val job = hbc.heartBeater.startIn(this)
        advanceTimeBy(TEST_RECEIVED_PERIOD_MILLIS)
        runCurrent()
        assertTrue(hbc.received.tryReceive().isSuccess, "should have received a heartbeat after 1st period of inactivity")

        advanceTimeBy(TEST_RECEIVED_PERIOD_MILLIS)
        runCurrent()
        assertTrue(hbc.received.tryReceive().isSuccess, "should have received a heartbeat after 2nd period of inactivity")

        repeat(3) {
            hbc.heartBeater.notifyMsgReceived()
            advanceTimeBy(TEST_RECEIVED_PERIOD_MILLIS / 2)
        }
        runCurrent()
        assertTrue(hbc.received.isEmpty, "shouldn't have received heart beat since messages were received")

        hbc.heartBeater.notifyMsgReceived()
        advanceTimeBy(TEST_RECEIVED_PERIOD_MILLIS)
        runCurrent()
        assertTrue(hbc.received.tryReceive().isSuccess, "should have received a heartbeat after 3rd period of inactivity")

        assertTrue(hbc.sent.isEmpty, "shouldn't have sent any heart beat")
        job.cancel()
    }

    @Test
    fun zeroSend_nonZeroReceive_canCallNotifySent() = runTest {
        val hbc = HeartBeaterConsumer(HeartBeat(ZERO, TEST_PERIOD_CONFIG))
        val job = hbc.heartBeater.startIn(this)
        hbc.heartBeater.notifyMsgSent()
        job.cancel()
    }
}
