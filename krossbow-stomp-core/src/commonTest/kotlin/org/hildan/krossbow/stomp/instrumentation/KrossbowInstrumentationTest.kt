package org.hildan.krossbow.stomp.instrumentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.hildan.krossbow.stomp.StompErrorFrameReceived
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.test.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class KrossbowInstrumentationTest {

    @Test
    fun testInstrumentation_sendAndNormalClosure() = runTest {
        val inst = KrossbowInstrumentationMock()

        launch {
            val (wsSession, stompSession) = connectWithMocks {
                instrumentation = inst
            }
            launch {
                wsSession.awaitSendFrameAndSimulateCompletion()
                wsSession.awaitDisconnectFrameAndSimulateCompletion()
                wsSession.expectClose()
            }
            stompSession.sendText("/dest", "Some Body")
            stompSession.disconnect()
        }

        val connectFrame = inst.expectOnStompFrameSent()
        assertEquals(StompCommand.CONNECT, connectFrame.command)

        inst.expectOnWebsocketFrameReceived()
        val connectedFrame = inst.expectOnFrameDecoded()
        assertEquals(StompCommand.CONNECTED, connectedFrame.command)

        val sendFrame = inst.expectOnStompFrameSent()
        assertEquals(StompCommand.SEND, sendFrame.command)
        assertEquals("Some Body", sendFrame.bodyAsText)

        val disconnectFrame = inst.expectOnStompFrameSent()
        assertEquals(StompCommand.DISCONNECT, disconnectFrame.command)

        val closeCause = inst.expectOnWebSocketClosed()
        assertNull(closeCause, "The web socket should be closed normally")
    }

    @Test
    fun testInstrumentation_webSocketError() = runTest {
        val inst = KrossbowInstrumentationMock()

        launch {
            val (wsSession, _) = connectWithMocks {
                instrumentation = inst
            }
            wsSession.simulateError("Simulated error")
            wsSession.expectNoClose() // we don't attempt to close the failed websocket
        }

        val connectFrame = inst.expectOnStompFrameSent()
        assertEquals(StompCommand.CONNECT, connectFrame.command)

        inst.expectOnWebsocketFrameReceived()
        val connectedFrame = inst.expectOnFrameDecoded()
        assertEquals(StompCommand.CONNECTED, connectedFrame.command)

        val exception = inst.expectOnWebSocketClientError()
        assertEquals("Simulated error", exception.message)
    }

    @Test
    fun testInstrumentation_stompErrorFrame() = runTest {
        val inst = KrossbowInstrumentationMock()

        launch {
            val (wsSession, _) = connectWithMocks {
                instrumentation = inst
            }
            wsSession.simulateErrorFrameReceived("Simulated error")
            wsSession.expectClose() // we should close the websocket on STOMP error
        }

        val connectFrame = inst.expectOnStompFrameSent()
        assertEquals(StompCommand.CONNECT, connectFrame.command)

        inst.expectOnWebsocketFrameReceived()
        val connectedFrame = inst.expectOnFrameDecoded()
        assertEquals(StompCommand.CONNECTED, connectedFrame.command)

        inst.expectOnWebsocketFrameReceived()
        val errorFrame = inst.expectOnFrameDecoded()
        assertEquals(StompCommand.ERROR, errorFrame.command)

        val closeCause = inst.expectOnWebSocketClosed()
        assertNotNull(closeCause, "The web socket should be closed with STOMP ERROR as cause")
        assertEquals("Simulated error", closeCause.message)
        assertEquals(StompErrorFrameReceived::class, closeCause::class)
    }
}
