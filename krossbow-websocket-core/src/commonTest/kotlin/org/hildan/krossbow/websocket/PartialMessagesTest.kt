package org.hildan.krossbow.websocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.io.*
import kotlinx.io.bytestring.*
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
        var result: ByteString? = null
        val handler = PartialBinaryMessageHandler { result = it }

        assertNull(result)

        val zeroOneTwo = ByteString(0, 1, 2)
        handler.processMessage(zeroOneTwo, isLast = true)
        assertEquals(zeroOneTwo, result)

        val oneTwo = ByteString(1, 2)
        val threeFourFive = ByteString(3, 4, 5)
        handler.processMessage(oneTwo, isLast = false)
        assertEquals(zeroOneTwo, result, "handler shouldn't trigger the callback on incomplete msg")
        handler.processMessage(threeFourFive, isLast = true)
        assertEquals(oneTwo + threeFourFive, result, "the complete msg should be sent when last part is received")

        val one = ByteString(1)
        val two = ByteString(2)
        val three = ByteString(3)
        handler.processMessage(one, isLast = false)
        assertEquals(oneTwo + threeFourFive, result, "handler shouldn't trigger the callback on incomplete msg")
        handler.processMessage(two, isLast = false)
        assertEquals(oneTwo + threeFourFive, result, "handler shouldn't trigger the callback on incomplete msg")
        handler.processMessage(three, isLast = true)
        assertEquals(one + two + three, result, "the complete msg should be sent when last part is received")
    }
}

private operator fun ByteString.plus(other: ByteString): ByteString {
    val buffer = Buffer().apply {
        write(this@plus)
        write(other)
    }
    return buffer.readByteString()
}
