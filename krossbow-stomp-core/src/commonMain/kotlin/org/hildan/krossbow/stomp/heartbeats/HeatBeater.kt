package org.hildan.krossbow.stomp.heartbeats

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.config.HeartBeatTolerance
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class HeartBeater(
    private val heartBeat: HeartBeat,
    private val tolerance: HeartBeatTolerance,
    private val sendHeartBeat: suspend () -> Unit,
    private val onMissingHeartBeat: suspend () -> Unit
) {
    private val outgoingTicker: Ticker = createOutgoingHeartBeatsTicker()
    private val incomingTicker: Ticker = createIncomingHeartBeatsTicker()

    fun startIn(scope: CoroutineScope): Job = scope.launch(CoroutineName("stomp-heart-beat")) {
        if (heartBeat.minSendPeriodMillis > 0) {
            outgoingTicker.startIn(this + CoroutineName("stomp-heart-beat-outgoing"))
        }
        if (heartBeat.expectedPeriodMillis > 0) {
            incomingTicker.startIn(this + CoroutineName("stomp-heart-beat-incoming"))
        }
    }

    private fun createIncomingHeartBeatsTicker(): Ticker {
        val incomingPeriodExpectation = heartBeat.expectedPeriodMillis + tolerance.incomingMarginMillis
        return Ticker(incomingPeriodExpectation.toLong(), onMissingHeartBeat)
    }

    private fun createOutgoingHeartBeatsTicker(): Ticker {
        val outgoingPeriodMillis = heartBeat.minSendPeriodMillis - tolerance.outgoingMarginMillis
        return Ticker(outgoingPeriodMillis.toLong(), sendHeartBeat)
    }

    fun notifyMsgSent() {
        outgoingTicker.reset()
    }

    fun notifyMsgReceived() {
        incomingTicker.reset()
    }
}

private class Ticker(
    val periodMillis: Long,
    val onTick: suspend () -> Unit
) {
    private val resetEvents = Channel<Unit>()

    @OptIn(ExperimentalCoroutinesApi::class) // for onTimeout
    fun startIn(scope: CoroutineScope): Job = scope.launch {
        while (isActive) {
            select<Unit> {
                resetEvents.onReceive { }
                onTimeout(periodMillis, onTick)
            }
        }
    }

    fun reset() {
        resetEvents.offer(Unit)
    }
}
