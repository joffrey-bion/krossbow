package org.hildan.krossbow.stomp.heartbeats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.config.HeartBeatTolerance
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val TEST_PERIOD_CONFIG = 5000
private const val TEST_OUTGOING_MARGIN = 200
private const val TEST_INCOMING_MARGIN = 500
private const val TEST_SEND_PERIOD: Long = (TEST_PERIOD_CONFIG - TEST_OUTGOING_MARGIN).toLong()
private const val TEST_RECEIVED_PERIOD: Long = (TEST_PERIOD_CONFIG + TEST_INCOMING_MARGIN).toLong()

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
    fun zeroSendAndReceive_nothingHappens() = runBlockingTest {
        val hbc = HeartBeaterConsumer(HeartBeat(0, 0))
        assertTrue(hbc.sent.isEmpty)
        assertTrue(hbc.received.isEmpty)
        hbc.heartBeater.startIn(this)
        advanceTimeBy(10000)
        assertTrue(hbc.sent.isEmpty)
        assertTrue(hbc.received.isEmpty)
    }

    @Test
    fun zeroSendAndReceive_canCallNotifyWithoutEffect() = runBlockingTest {
        val hbc = HeartBeaterConsumer(HeartBeat(0, 0))
        hbc.heartBeater.startIn(this)
        hbc.heartBeater.notifyMsgReceived()
        hbc.heartBeater.notifyMsgSent()
    }

    @Test
    fun nonZeroSend_zeroReceive_sendsHeartBeats() = runBlockingTest {
        val hbc = HeartBeaterConsumer(HeartBeat(TEST_PERIOD_CONFIG, 0))
        assertTrue(hbc.sent.isEmpty, "shouldn't do anything if not started")

        val job = hbc.heartBeater.startIn(this)
        assertTrue(hbc.sent.isEmpty, "should NOT have sent a heartbeat right away")
        advanceTimeBy(TEST_SEND_PERIOD - 1)
        assertTrue(hbc.sent.isEmpty, "should NOT have sent a heartbeat before 1st period of inactivity")
        advanceTimeBy(1)
        assertNotNull(hbc.sent.poll(), "should have sent a heartbeat after 1st period of inactivity")
        advanceTimeBy(TEST_SEND_PERIOD)
        assertNotNull(hbc.sent.poll(), "should have sent a heartbeat after 2nd period of inactivity")

        repeat(3) {
            hbc.heartBeater.notifyMsgSent()
            advanceTimeBy(TEST_SEND_PERIOD / 2)
        }
        assertTrue(hbc.sent.isEmpty, "shouldn't have sent any heart beat since messages were sent")

        hbc.heartBeater.notifyMsgSent()
        advanceTimeBy(TEST_SEND_PERIOD)
        assertNotNull(hbc.sent.poll(), "should have sent a heartbeat after 3rd period of inactivity")

        assertTrue(hbc.received.isEmpty, "shouldn't have received any heart beat")
        job.cancel()
    }

    @Test
    fun nonZeroSend_zeroReceive_canCallNotifyReceived() = runBlockingTest {
        val hbc = HeartBeaterConsumer(HeartBeat(TEST_PERIOD_CONFIG, 0))
        val job = hbc.heartBeater.startIn(this)
        hbc.heartBeater.notifyMsgReceived()
        job.cancel()
    }

    @Test
    fun zeroSend_nonZeroReceive_sends() = runBlockingTest {
        val hbc = HeartBeaterConsumer(HeartBeat(0, TEST_PERIOD_CONFIG))
        assertTrue(hbc.received.isEmpty, "shouldn't do anything if not started")

        val job = hbc.heartBeater.startIn(this)
        delay(TEST_RECEIVED_PERIOD)
        assertNotNull(hbc.received.poll(), "should have received a heartbeat after 1st period of inactivity")
        delay(TEST_RECEIVED_PERIOD)
        assertNotNull(hbc.received.poll(), "should have received a heartbeat after 2nd period of inactivity")
        repeat(3) {
            hbc.heartBeater.notifyMsgReceived()
            delay(TEST_RECEIVED_PERIOD / 2)
        }
        assertTrue(hbc.received.isEmpty, "shouldn't have received heart beat since messages were received")

        hbc.heartBeater.notifyMsgReceived()
        delay(TEST_RECEIVED_PERIOD)
        assertNotNull(hbc.received.poll(), "should have received a heartbeat after 3rd period of inactivity")

        assertTrue(hbc.sent.isEmpty, "shouldn't have sent any heart beat")
        job.cancel()
    }

    @Test
    fun zeroSend_nonZeroReceive_canCallNotifySent() = runBlockingTest {
        val hbc = HeartBeaterConsumer(HeartBeat(0, TEST_PERIOD_CONFIG))
        val job = hbc.heartBeater.startIn(this)
        hbc.heartBeater.notifyMsgSent()
        job.cancel()
    }
}
