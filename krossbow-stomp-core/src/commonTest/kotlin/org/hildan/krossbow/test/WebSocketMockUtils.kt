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
import org.hildan.krossbow.websocket.test.*
import kotlin.coroutines.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

suspend fun connectWithMocks(
    connectedHeaders: StompConnectedHeaders = StompConnectedHeaders(),
    wsSelectedProtocol: String? = null,
    configure: StompConfig.() -> Unit = {},
): Pair<WebSocketConnectionMock, StompSession> = coroutineScope {
    val wsClient = WebSocketClientMock()
    val stompClient = StompClient(wsClient) {
        configure()
        // to use the test dispatcher
        defaultSessionCoroutineContext = coroutineContext[ContinuationInterceptor] ?: EmptyCoroutineContext
    }
    val deferredStompSession = async { stompClient.connect("dummy URL") }
    val wsSession = wsClient.awaitConnectAndSimulateSuccess(selectedProtocol = wsSelectedProtocol)
    wsSession.awaitConnectFrameAndSimulateCompletion()
    wsSession.simulateConnectedFrameReceived(connectedHeaders)
    val stompSession = deferredStompSession.await()
    Pair(wsSession, stompSession)
}

suspend fun WebSocketConnectionMock.simulateTextStompFrameReceived(frame: StompFrame) {
    simulateTextFrameReceived(frame.encodeToText())
}

suspend fun WebSocketConnectionMock.simulateBinaryStompFrameReceived(frame: StompFrame) {
    simulateBinaryFrameReceived(frame.encodeToByteString())
}

suspend fun WebSocketConnectionMock.simulateErrorFrameReceived(errorMessage: String): StompFrame.Error {
    val errorFrame = StompFrame.Error(StompErrorHeaders { message = errorMessage }, null)
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
suspend fun WebSocketConnectionMock.awaitSentStompFrameAndSimulateCompletion(): StompFrame {
    return when (val wsFrame = waitForSentWsFrameAndSimulateCompletion()) {
        is WebSocketFrame.Binary -> wsFrame.bytes.decodeToStompFrame()
        is WebSocketFrame.Text -> wsFrame.text.decodeToStompFrame()
        else -> error("The web socket frame is not a data frame: ${wsFrame::class.simpleName}")
    }
}

suspend inline fun <reified T : StompFrame> WebSocketConnectionMock.awaitSentStompFrameAndSimulateCompletion(
    expectedCommand: StompCommand
): T {
    val frame = awaitSentStompFrameAndSimulateCompletion()
    assertEquals(expectedCommand, frame.command, "The next sent frame should be a $expectedCommand STOMP frame.")
    assertTrue(frame is T, "The next sent frame should be of type ${T::class.simpleName}")
    return frame
}

suspend fun WebSocketConnectionMock.awaitConnectFrameAndSimulateCompletion(): StompFrame.Connect =
    awaitSentStompFrameAndSimulateCompletion(StompCommand.CONNECT)

suspend fun WebSocketConnectionMock.awaitSendFrameAndSimulateCompletion(): StompFrame.Send =
    awaitSentStompFrameAndSimulateCompletion(StompCommand.SEND)

suspend fun WebSocketConnectionMock.awaitBeginFrameAndSimulateCompletion(): StompFrame.Begin =
    awaitSentStompFrameAndSimulateCompletion(StompCommand.BEGIN)

suspend fun WebSocketConnectionMock.awaitCommitFrameAndSimulateCompletion(): StompFrame.Commit =
    awaitSentStompFrameAndSimulateCompletion(StompCommand.COMMIT)

suspend fun WebSocketConnectionMock.awaitAbortFrameAndSimulateCompletion(): StompFrame.Abort =
    awaitSentStompFrameAndSimulateCompletion(StompCommand.ABORT)

suspend fun WebSocketConnectionMock.awaitSubscribeFrameAndSimulateCompletion(): StompFrame.Subscribe =
    awaitSentStompFrameAndSimulateCompletion(StompCommand.SUBSCRIBE)

suspend fun WebSocketConnectionMock.awaitUnsubscribeFrameAndSimulateCompletion(expectedSubId: String): StompFrame.Unsubscribe {
    val frame = awaitSentStompFrameAndSimulateCompletion<StompFrame.Unsubscribe>(StompCommand.UNSUBSCRIBE)
    assertEquals(expectedSubId, frame.headers.id, "The subscription ID doesn't match")
    return frame
}

suspend fun WebSocketConnectionMock.awaitDisconnectFrameAndSimulateCompletion(): StompFrame.Disconnect =
    awaitSentStompFrameAndSimulateCompletion(StompCommand.DISCONNECT)
