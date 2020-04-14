package org.hildan.krossbow.stomp.heartbeats

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.test.runAsyncTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HeartBeaterTest {

    @Test
    fun zeroSendAndReceive_nothingHappens() = runAsyncTest {
        var result = 0
        val heartBeater = HeartBeater(
            heartBeat = HeartBeat(0, 0),
            sendHeartBeat = { result++ },
            onMissingHeartBeat = { throw IllegalStateException("not expected") }
        )
        assertEquals(0, result)
        heartBeater.startIn(this)
        delay(100)
        assertEquals(0, result)
    }

    @Test
    fun zeroSendAndReceive_canCallNotifyWithoutEffect() = runAsyncTest {
        val heartBeater = HeartBeater(
            heartBeat = HeartBeat(0, 0),
            sendHeartBeat = { throw IllegalStateException("not expected") },
            onMissingHeartBeat = { throw IllegalStateException("not expected") }
        )
        heartBeater.startIn(this)
        withTimeout(100) { heartBeater.notifyMsgReceived() }
        withTimeout(100) { heartBeater.notifyMsgSent() }
    }

    @Test
    fun nonZeroSend_zeroReceive_sendsHeartBeats() = runAsyncTest {
        val sendPeriod = 100L
        var result = 0
        val heartBeater = HeartBeater(
            heartBeat = HeartBeat(sendPeriod.toInt(), 0),
            sendHeartBeat = { result++ },
            onMissingHeartBeat = { throw IllegalStateException("not expected") }
        )
        assertEquals(0, result, "shouldn't do anything if not started")
        val job = heartBeater.startIn(this)
        delay(sendPeriod + 20)
        assertEquals(1, result, "should send heartbeat after 1 period of inactivity")
        delay(sendPeriod)
        assertEquals(2, result, "should send heartbeat after 2 periods of inactivity")
        delay(sendPeriod / 2)
        heartBeater.notifyMsgSent()
        assertEquals(2, result, "should not send heartbeat if a message was sent")
        delay(sendPeriod / 2)
        heartBeater.notifyMsgSent()
        assertEquals(2, result, "should not send heartbeat if a message was sent")
        delay(sendPeriod / 2)
        heartBeater.notifyMsgSent()
        assertEquals(2, result, "should not send heartbeat if a message was sent")
        delay(sendPeriod + 20)
        assertEquals(3, result, "should send heartbeat after 1 period of inactivity")
        job.cancel()
    }

    @Test
    fun nonZeroSend_zeroReceive_canCallNotifyReceived() = runAsyncTest {
        val sendPeriod = 60L
        val heartBeater = HeartBeater(
            heartBeat = HeartBeat(sendPeriod.toInt(), 0),
            sendHeartBeat = {},
            onMissingHeartBeat = { throw IllegalStateException("not expected") }
        )
        val job = heartBeater.startIn(this)
        withTimeout(100) { heartBeater.notifyMsgReceived() }
        job.cancel()
    }

    @Test
    fun zeroSend_nonZeroReceive_sends() = runAsyncTest {
        val receivePeriod = 100L
        var result = 0
        val heartBeater = HeartBeater(
            heartBeat = HeartBeat(0, receivePeriod.toInt()),
            sendHeartBeat = { throw IllegalStateException("not expected") },
            onMissingHeartBeat = { result++ }
        )
        assertEquals(0, result, "shouldn't do anything if not started")
        val job = heartBeater.startIn(this)
        delay(receivePeriod + 20)
        assertEquals(1, result, "should notify missing heartbeat after 1 period of inactivity")
        delay(receivePeriod)
        assertEquals(2, result, "should notify missing heartbeat after 2 periods of inactivity")
        delay(receivePeriod / 2)
        heartBeater.notifyMsgReceived()
        assertEquals(2, result, "should not notify missing heartbeat if a message was received")
        delay(receivePeriod / 2)
        heartBeater.notifyMsgReceived()
        assertEquals(2, result, "should not notify missing heartbeat if a message was received")
        delay(receivePeriod / 2)
        heartBeater.notifyMsgReceived()
        assertEquals(2, result, "should not notify missing heartbeat if a message was received")
        delay(receivePeriod + 20)
        assertEquals(3, result, "should send heartbeat after 1 period of inactivity")
        job.cancel()
    }

    @Test
    fun zeroSend_nonZeroReceive_canCallNotifySent() = runAsyncTest {
        val receivePeriod = 60L
        val heartBeater = HeartBeater(
            heartBeat = HeartBeat(0, receivePeriod.toInt()),
            sendHeartBeat = { throw IllegalStateException("not expected") },
            onMissingHeartBeat = {}
        )
        val job = heartBeater.startIn(this)
        withTimeout(100) { heartBeater.notifyMsgSent() }
        job.cancel()
    }
}
