package org.hildan.krossbow.stomp.heartbeats

import org.hildan.krossbow.stomp.config.HeartBeat

internal val NO_HEART_BEATS = HeartBeat(0, 0)

internal fun HeartBeat.negotiated(serverHeartBeats: HeartBeat?): HeartBeat = HeartBeat(
    minSendPeriodMillis = computeNegotiatedPeriod(minSendPeriodMillis, serverHeartBeats?.expectedPeriodMillis),
    expectedPeriodMillis = computeNegotiatedPeriod(expectedPeriodMillis, serverHeartBeats?.minSendPeriodMillis),
)

private fun computeNegotiatedPeriod(clientPeriod: Int, serverPeriod: Int?): Int = when {
    serverPeriod == null || serverPeriod == 0 || clientPeriod == 0 -> 0
    else -> maxOf(clientPeriod, serverPeriod)
}
