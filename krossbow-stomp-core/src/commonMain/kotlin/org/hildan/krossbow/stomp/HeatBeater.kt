package org.hildan.krossbow.stomp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.hildan.krossbow.stomp.config.HeartBeat

internal class HeartBeater(
    val heartBeat: HeartBeat,
    val sendHeartBeat: suspend () -> Unit,
    val onMissingHeartBeat: suspend () -> Unit
) {
    private val outgoing = Ticker((heartBeat.minSendPeriodMillis * 0.95).toLong(), sendHeartBeat)
    private val incoming = Ticker((heartBeat.expectedPeriodMillis * 1.05).toLong(), onMissingHeartBeat)

    fun startIn(scope: CoroutineScope): Job = scope.launch {
        if (heartBeat.minSendPeriodMillis > 0) {
            outgoing.startIn(scope)
        }
        if (heartBeat.expectedPeriodMillis > 0) {
            incoming.startIn(scope)
        }
    }

    suspend fun notifyMsgSent() {
        outgoing.reset()
    }

    suspend fun notifyMsgReceived() {
        incoming.reset()
    }
}

private class Ticker(
    private val periodMillis: Long,
    private val onTick: suspend () -> Unit
) {
    private val resetter = Channel<Unit>()

    @OptIn(ExperimentalCoroutinesApi::class) // for onTimeout
    fun startIn(scope: CoroutineScope): Job = scope.launch {
        while (isActive) {
            select<Unit> {
                resetter.onReceive { }
                onTimeout(periodMillis, onTick)
            }
        }
    }

    suspend fun reset() {
        resetter.send(Unit)
    }
}
