package org.hildan.krossbow.websocket.reconnection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@OptIn(ExperimentalTime::class)
internal class RetryDelayStrategyTest {

    @Test
    fun fixedDelay_1s() {
        val eb = FixedDelay(1.seconds)
        assertEquals(1.seconds, eb.computeDelay(0))
        assertEquals(1.seconds, eb.computeDelay(1))
        assertEquals(1.seconds, eb.computeDelay(2))
        assertEquals(1.seconds, eb.computeDelay(3))
    }

    @Test
    fun fixedDelay_500ms() {
        val eb = FixedDelay(500.milliseconds)
        assertEquals(500.milliseconds, eb.computeDelay(0))
        assertEquals(500.milliseconds, eb.computeDelay(1))
        assertEquals(500.milliseconds, eb.computeDelay(2))
        assertEquals(500.milliseconds, eb.computeDelay(3))
    }

    @Test
    fun exponentialBackoff_default_1s_factor2() {
        val eb = ExponentialBackOff()
        assertEquals(1.seconds, eb.computeDelay(0))
        assertEquals(2.seconds, eb.computeDelay(1))
        assertEquals(4.seconds, eb.computeDelay(2))
        assertEquals(8.seconds, eb.computeDelay(3))
    }

    @Test
    fun exponentialBackoff_custom_3s_factor1dot5() {
        val eb = ExponentialBackOff(3.seconds, 1.5)
        assertEquals(3.seconds, eb.computeDelay(0))
        assertEquals(4.5.seconds, eb.computeDelay(1))
        assertEquals(6.75.seconds, eb.computeDelay(2))
    }

    @Test
    fun exponentialBackoff_custom_4s_factor1dot5() {
        val eb = ExponentialBackOff(4.seconds, 1.5)
        assertEquals(4.seconds, eb.computeDelay(0))
        assertEquals(6.seconds, eb.computeDelay(1))
        assertEquals(9.seconds, eb.computeDelay(2))
        assertEquals(13.5.seconds, eb.computeDelay(3))
    }
}
