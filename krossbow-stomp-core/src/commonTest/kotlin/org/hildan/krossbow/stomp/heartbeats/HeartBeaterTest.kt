package org.hildan.krossbow.stomp.heartbeats

import kotlinx.coroutines.delay
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.test.runAsyncTestWithTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class HeartBeaterTest {

    @Test
    fun zeroSendAndReceive_nothingHappens() = runAsyncTestWithTimeout {
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
    fun zeroSendAndReceive_canCallNotifyWithoutEffect() = runAsyncTestWithTimeout {
        val heartBeater = HeartBeater(
            heartBeat = HeartBeat(0, 0),
            sendHeartBeat = { throw IllegalStateException("not expected") },
            onMissingHeartBeat = { throw IllegalStateException("not expected") }
        )
        heartBeater.startIn(this)
        heartBeater.notifyMsgReceived()
        heartBeater.notifyMsgSent()
    }

    @Test
    fun nonZeroSend_zeroReceive_sendsHeartBeats() = runAsyncTestWithTimeout(1500) {
        val sendPeriod = 200L
        var result = 0
        val heartBeater = HeartBeater(
            heartBeat = HeartBeat(sendPeriod.toInt(), 0),
            sendHeartBeat = { result++ },
            onMissingHeartBeat = { throw IllegalStateException("not expected") }
        )
        assertEquals(0, result, "shouldn't do anything if not started")
        val job = heartBeater.startIn(this)
        delay(sendPeriod + 50)
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
        delay(sendPeriod + 50)
        assertEquals(3, result, "should send heartbeat after 1 period of inactivity")
        job.cancel()
    }

    @Test
    fun nonZeroSend_zeroReceive_canCallNotifyReceived() = runAsyncTestWithTimeout {
        val sendPeriod = 60L
        val heartBeater = HeartBeater(
            heartBeat = HeartBeat(sendPeriod.toInt(), 0),
            sendHeartBeat = {},
            onMissingHeartBeat = { throw IllegalStateException("not expected") }
        )
        val job = heartBeater.startIn(this)
        heartBeater.notifyMsgReceived()
        job.cancel()
    }

    @Test
    fun zeroSend_nonZeroReceive_sends() = runAsyncTestWithTimeout(1500) {
        val receivePeriod = 200L
        var result = 0
        val heartBeater = HeartBeater(
            heartBeat = HeartBeat(0, receivePeriod.toInt()),
            sendHeartBeat = { throw IllegalStateException("not expected") },
            onMissingHeartBeat = { result++ }
        )
        assertEquals(0, result, "shouldn't do anything if not started")
        val job = heartBeater.startIn(this)
        delay(receivePeriod + 50)
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
    fun zeroSend_nonZeroReceive_canCallNotifySent() = runAsyncTestWithTimeout {
        val receivePeriod = 60L
        val heartBeater = HeartBeater(
            heartBeat = HeartBeat(0, receivePeriod.toInt()),
            sendHeartBeat = { throw IllegalStateException("not expected") },
            onMissingHeartBeat = {}
        )
        val job = heartBeater.startIn(this)
        heartBeater.notifyMsgSent()
        job.cancel()
    }
}
