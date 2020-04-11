package org.hildan.krossbow.websocket

import org.hildan.krossbow.websocket.test.runSuspendingTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PartialMessagesTest {

    @Test
    fun test() = runSuspendingTest {
        var result: String? = null
        val handler = PartialTextMessageHandler { result = it.toString() }

        assertNull(result)

        handler.processMessage("complete", isLast = true)
        assertEquals("complete", result)

        handler.processMessage("begin", isLast = false)
        assertEquals("complete", result, "handler shouldn't trigger the callback on incomplete msg")
        handler.processMessage("end", isLast = true)
        assertEquals("beginend", result, "the complete msg should be sent when last part is received")

        handler.processMessage("1", isLast = false)
        assertEquals("beginend", result, "handler shouldn't trigger the callback on incomplete msg")
        handler.processMessage("2", isLast = false)
        assertEquals("beginend", result, "handler shouldn't trigger the callback on incomplete msg")
        handler.processMessage("3", isLast = true)
        assertEquals("123", result, "the complete msg should be sent when last part is received")
    }
}
