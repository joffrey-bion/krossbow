package org.hildan.krossbow.test

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.*
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.stomp.headers.StompErrorHeaders
import org.hildan.krossbow.stomp.headers.StompMessageHeaders
import org.hildan.krossbow.stomp.headers.StompReceiptHeaders
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.test.WebSocketConnectionMock
import org.hildan.krossbow.websocket.test.webSocketClientMock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

suspend fun connectWithMocks(
    connectedHeaders: StompConnectedHeaders = StompConnectedHeaders(),
    configure: StompConfig.() -> Unit = {},
): Pair<WebSocketConnectionMock, StompSession> = coroutineScope {
    val wsSession = WebSocketConnectionMock()
    val stompClient = StompClient(webSocketClientMock { wsSession }, configure)
    val session = async { stompClient.connect("dummy URL") }
    wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
    wsSession.simulateConnectedFrameReceived(connectedHeaders)
    val stompSession = session.await()
    Pair(wsSession, stompSession)
}

suspend fun WebSocketConnectionMock.simulateTextStompFrameReceived(frame: StompFrame) {
    simulateTextFrameReceived(frame.encodeToText())
}

suspend fun WebSocketConnectionMock.simulateBinaryStompFrameReceived(frame: StompFrame) {
    simulateBinaryFrameReceived(frame.encodeToBytes())
}

suspend fun WebSocketConnectionMock.simulateErrorFrameReceived(errorMessage: String): StompFrame.Error {
    val errorFrame = StompFrame.Error(StompErrorHeaders(errorMessage), null)
    val result = runCatching {
        simulateTextStompFrameReceived(errorFrame)
    }
    assertTrue(
        result.isSuccess,
        "Calling the listener with an error frame is the responsibility of the web socket implementation, and " +
            "is done from a thread that we don't control, so we don't want that to fail."
    )
    return errorFrame
}

suspend fun WebSocketConnectionMock.simulateMessageFrameReceived(
    subId: String,
    body: String?,
    destination: String = "/destination",
    messageId: String = "42"
): StompFrame.Message {
    val headers = StompMessageHeaders(destination, messageId, subId)
    val frame = StompFrame.Message(headers, body?.let { FrameBody.Text(it) })
    simulateTextStompFrameReceived(frame)
    return frame
}

suspend fun WebSocketConnectionMock.simulateConnectedFrameReceived(
    connectedHeaders: StompConnectedHeaders = StompConnectedHeaders()
) {
    val connectedFrame = StompFrame.Connected(connectedHeaders)
    simulateTextStompFrameReceived(connectedFrame)
}

suspend fun WebSocketConnectionMock.simulateReceiptFrameReceived(receiptId: String) {
    simulateTextStompFrameReceived(StompFrame.Receipt(StompReceiptHeaders(receiptId)))
}

/**
 * Waits for a web socket frame to be sent, unblocking any send call.
 *
 * @returns the parsed stomp frame that was sent to allow further assertions
 */
private suspend fun WebSocketConnectionMock.waitForSentStompFrameAndSimulateCompletion(): StompFrame {
    return when (val wsFrame = waitForSentWsFrameAndSimulateCompletion()) {
        is WebSocketFrame.Binary -> StompDecoder.decode(wsFrame.bytes)
        is WebSocketFrame.Text -> StompDecoder.decode(wsFrame.text)
        else -> error("The web socket frame is not a data frame: ${wsFrame::class.simpleName}")
    }
}

suspend fun WebSocketConnectionMock.waitForSendAndSimulateCompletion(expectedCommand: StompCommand): StompFrame {
    val frame = waitForSentStompFrameAndSimulateCompletion()
    assertEquals(expectedCommand, frame.command, "The next sent frame should be a $expectedCommand STOMP frame.")
    return frame
}

suspend fun WebSocketConnectionMock.waitForSubscribeAndSimulateCompletion(): StompFrame.Subscribe {
    val frame = waitForSendAndSimulateCompletion(StompCommand.SUBSCRIBE)
    assertTrue(frame is StompFrame.Subscribe, "The next sent frame should be of type StompFrame.Subscribe")
    return frame
}

suspend fun WebSocketConnectionMock.waitForUnsubscribeAndSimulateCompletion(expectedSubId: String): StompFrame.Unsubscribe {
    val frame = waitForSendAndSimulateCompletion(StompCommand.UNSUBSCRIBE)
    assertTrue(frame is StompFrame.Unsubscribe, "The next sent frame should be of type StompFrame.Unsubscribe")
    assertEquals(expectedSubId, frame.headers.id, "The subscription ID doesn't match")
    return frame
}

suspend fun WebSocketConnectionMock.waitForDisconnectAndSimulateCompletion(): StompFrame.Disconnect {
    val frame = waitForSendAndSimulateCompletion(StompCommand.DISCONNECT)
    assertTrue(frame is StompFrame.Disconnect, "The next sent frame should be of type StompFrame.Disconnect")
    return frame
}
