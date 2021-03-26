package org.hildan.krossbow.websocket.reconnection

import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

/**
 * Defines the time to wait before each attempt in a retry mechanism.
 */
interface RetryDelayStrategy {

    /**
     * Calculates the time to wait before the given [attempt].
     *
     * The first attempt has number 0, the next one is 1, etc.
     */
    @OptIn(ExperimentalTime::class)
    fun computeDelay(attempt: Int): Duration
}

/**
 * A [RetryDelayStrategy] where the delay is the same for all attempts.
 */
@OptIn(ExperimentalTime::class)
class FixedDelay(private val delay: Duration): RetryDelayStrategy {

    override fun computeDelay(attempt: Int): Duration = delay
}

/**
 * A [RetryDelayStrategy] where the delay is multiplied by a constant factor after each attempt.
 */
@OptIn(ExperimentalTime::class)
class ExponentialBackOff(
    /**
     * The time to wait before the first attempt.
     */
    private val initialDelay: Duration = 1.seconds,
    /**
     * The multiplier to use to increase the delay for subsequent attempts.
     */
    private val factor: Double = 2.0,
): RetryDelayStrategy {

    override fun computeDelay(attempt: Int): Duration = initialDelay * factor.pow(attempt)
}