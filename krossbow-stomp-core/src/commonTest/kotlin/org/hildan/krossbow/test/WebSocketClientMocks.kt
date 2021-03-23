package org.hildan.krossbow.test

import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.StompCommand
import org.hildan.krossbow.stomp.headers.StompConnectedHeaders
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection

suspend fun connectWithMocks(
    connectedHeaders: StompConnectedHeaders = StompConnectedHeaders(),
    configure: StompConfig.() -> Unit = {},
): Pair<WebSocketConnectionMock, StompSession> = coroutineScope {
    val wsSession = WebSocketConnectionMock()
    val stompClient = StompClient(ImmediatelySucceedingWebSocketClient(wsSession), configure)
    val session = async { stompClient.connect("dummy URL") }
    wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
    wsSession.simulateConnectedFrameReceived(connectedHeaders)
    val stompSession = session.await()
    Pair(wsSession, stompSession)
}

class ImmediatelySucceedingWebSocketClient(
    private val session: WebSocketConnectionMock = WebSocketConnectionMock()
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketConnection = session
}

class ImmediatelyFailingWebSocketClient(private val exception: Throwable) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketConnection = throw exception
}

class ManuallyConnectingWebSocketClient : WebSocketClient {

    private val connectEventChannel = Channel<Unit>()
    private val connectedEventChannel = Channel<WebSocketConnection>()

    override suspend fun connect(url: String): WebSocketConnection {
        connectEventChannel.send(Unit)
        return connectedEventChannel.receive()
    }

    suspend fun waitForConnectCall() {
        connectEventChannel.receive()
    }

    suspend fun simulateSuccessfulConnection(session: WebSocketConnectionMock) {
        connectedEventChannel.send(session)
    }
}
