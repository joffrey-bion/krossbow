package org.hildan.krossbow.stomp.heartbeats

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.*
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.config.HeartBeatTolerance
import kotlin.time.Duration

internal class HeartBeater(
    private val heartBeat: HeartBeat,
    tolerance: HeartBeatTolerance,
    sendHeartBeat: suspend () -> Unit,
    onMissingHeartBeat: suspend () -> Unit,
) {
    private val outgoingTicker = Ticker(heartBeat.minSendPeriod - tolerance.outgoingMargin, sendHeartBeat)
    private val incomingTicker = Ticker(heartBeat.expectedPeriod + tolerance.incomingMargin, onMissingHeartBeat)

    fun startIn(scope: CoroutineScope): Job = scope.launch(CoroutineName("stomp-heart-beat")) {
        if (heartBeat.minSendPeriod > Duration.ZERO) {
            outgoingTicker.startIn(this + CoroutineName("stomp-heart-beat-outgoing"))
        }
        if (heartBeat.expectedPeriod > Duration.ZERO) {
            incomingTicker.startIn(this + CoroutineName("stomp-heart-beat-incoming"))
        }
    }

    fun notifyMsgSent() {
        outgoingTicker.reset()
    }

    fun notifyMsgReceived() {
        incomingTicker.reset()
    }
}

private class Ticker(
    val period: Duration,
    val onTick: suspend () -> Unit,
) {
    private val resetEvents = Channel<Unit>()

    @OptIn(ExperimentalCoroutinesApi::class) // for onTimeout
    fun startIn(scope: CoroutineScope): Job = scope.launch {
        while (isActive) {
            select<Unit> {
                resetEvents.onReceive { }
                onTimeout(period, onTick)
            }
        }
    }

    fun reset() {
        resetEvents.trySend(Unit)
    }
}
