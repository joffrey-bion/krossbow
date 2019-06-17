package org.hildan.krossbow.client

import org.hildan.krossbow.engines.webstompjs.WebstompKrossbowEngine
import kotlin.test.Test
import kotlin.test.assertEquals

class JsDefaultEngineTest {

    @Test
    fun testDefaultEngine() {
        assertEquals(WebstompKrossbowEngine, defaultEngine())
    }
}
