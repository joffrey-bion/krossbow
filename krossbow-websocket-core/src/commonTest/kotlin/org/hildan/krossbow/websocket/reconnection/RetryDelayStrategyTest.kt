package org.hildan.krossbow.websocket.reconnection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class RetryDelayStrategyTest {

    @Test
    fun fixedDelay_1s() {
        val eb = FixedDelay(Duration.seconds(1))
        assertEquals(Duration.seconds(1), eb.computeDelay(0))
        assertEquals(Duration.seconds(1), eb.computeDelay(1))
        assertEquals(Duration.seconds(1), eb.computeDelay(2))
        assertEquals(Duration.seconds(1), eb.computeDelay(3))
    }

    @Test
    fun fixedDelay_500ms() {
        val eb = FixedDelay(Duration.milliseconds(500))
        assertEquals(Duration.milliseconds(500), eb.computeDelay(0))
        assertEquals(Duration.milliseconds(500), eb.computeDelay(1))
        assertEquals(Duration.milliseconds(500), eb.computeDelay(2))
        assertEquals(Duration.milliseconds(500), eb.computeDelay(3))
    }

    @Test
    fun exponentialBackoff_default_1s_factor2() {
        val eb = ExponentialBackOff()
        assertEquals(Duration.seconds(1), eb.computeDelay(0))
        assertEquals(Duration.seconds(2), eb.computeDelay(1))
        assertEquals(Duration.seconds(4), eb.computeDelay(2))
        assertEquals(Duration.seconds(8), eb.computeDelay(3))
    }

    @Test
    fun exponentialBackoff_custom_3s_factor1dot5() {
        val eb = ExponentialBackOff(Duration.seconds(3), 1.5)
        assertEquals(Duration.seconds(3), eb.computeDelay(0))
        assertEquals(Duration.seconds(4.5), eb.computeDelay(1))
        assertEquals(Duration.seconds(6.75), eb.computeDelay(2))
    }

    @Test
    fun exponentialBackoff_custom_4s_factor1dot5() {
        val eb = ExponentialBackOff(Duration.seconds(4), 1.5)
        assertEquals(Duration.seconds(4), eb.computeDelay(0))
        assertEquals(Duration.seconds(6), eb.computeDelay(1))
        assertEquals(Duration.seconds(9), eb.computeDelay(2))
        assertEquals(Duration.seconds(13.5), eb.computeDelay(3))
    }
}
