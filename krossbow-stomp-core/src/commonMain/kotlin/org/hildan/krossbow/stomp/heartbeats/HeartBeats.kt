package org.hildan.krossbow.stomp.heartbeats

import org.hildan.krossbow.stomp.config.HeartBeat
import kotlin.native.concurrent.SharedImmutable
import kotlin.time.Duration

@SharedImmutable
internal val NO_HEART_BEATS = HeartBeat(Duration.ZERO, Duration.ZERO)

internal fun HeartBeat.negotiated(serverHeartBeats: HeartBeat?): HeartBeat = HeartBeat(
    minSendPeriod = computeNegotiatedPeriod(minSendPeriod, serverHeartBeats?.expectedPeriod),
    expectedPeriod = computeNegotiatedPeriod(expectedPeriod, serverHeartBeats?.minSendPeriod),
)

private fun computeNegotiatedPeriod(clientPeriod: Duration, serverPeriod: Duration?): Duration = when {
    serverPeriod == null || serverPeriod == Duration.ZERO || clientPeriod == Duration.ZERO -> Duration.ZERO
    else -> maxOf(clientPeriod, serverPeriod)
}
