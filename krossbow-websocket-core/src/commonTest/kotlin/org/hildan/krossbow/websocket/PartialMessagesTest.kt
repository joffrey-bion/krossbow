package org.hildan.krossbow.websocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class) // for runTest
class PartialMessagesTest {

    @Test
    fun testTextHandler() = runTest {
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

    @Test
    fun testBinaryHandler() = runTest {
        var result: List<Byte>? = null
        val handler = PartialBinaryMessageHandler { result = it.toList() }

        assertNull(result)

        val zeroOneTwo = listOf<Byte>(0, 1, 2)
        handler.processMessage(zeroOneTwo.toByteArray(), isLast = true)
        assertEquals(zeroOneTwo, result)

        val oneTwo = listOf<Byte>(1, 2)
        val threeFourFive = listOf<Byte>(3, 4, 5)
        handler.processMessage(oneTwo.toByteArray(), isLast = false)
        assertEquals(zeroOneTwo, result, "handler shouldn't trigger the callback on incomplete msg")
        handler.processMessage(threeFourFive.toByteArray(), isLast = true)
        assertEquals(oneTwo + threeFourFive, result, "the complete msg should be sent when last part is received")

        val one = listOf<Byte>(1)
        val two = listOf<Byte>(2)
        val three = listOf<Byte>(3)
        handler.processMessage(one.toByteArray(), isLast = false)
        assertEquals(oneTwo + threeFourFive, result, "handler shouldn't trigger the callback on incomplete msg")
        handler.processMessage(two.toByteArray(), isLast = false)
        assertEquals(oneTwo + threeFourFive, result, "handler shouldn't trigger the callback on incomplete msg")
        handler.processMessage(three.toByteArray(), isLast = true)
        assertEquals(one + two + three, result, "the complete msg should be sent when last part is received")
    }
}
