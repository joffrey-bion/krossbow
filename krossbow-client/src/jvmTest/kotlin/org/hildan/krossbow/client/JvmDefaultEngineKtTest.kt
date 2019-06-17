package org.hildan.krossbow.client

import org.hildan.krossbow.engines.spring.SpringKrossbowEngine
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmDefaultEngineKtTest {

    @Test
    fun defaultEngineTest() {
        assertEquals(SpringKrossbowEngine, defaultEngine())
    }
}
