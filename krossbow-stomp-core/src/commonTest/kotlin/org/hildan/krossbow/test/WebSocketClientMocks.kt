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
import org.hildan.krossbow.websocket.WebSocketSession

suspend fun connectWithMocks(
    connectedHeaders: StompConnectedHeaders = StompConnectedHeaders(),
    configure: StompConfig.() -> Unit = {},
): Pair<WebSocketSessionMock, StompSession> = coroutineScope {
    val wsSession = WebSocketSessionMock()
    val stompClient = StompClient(ImmediatelySucceedingWebSocketClient(wsSession), configure)
    val session = async { stompClient.connect("dummy URL") }
    wsSession.waitForSendAndSimulateCompletion(StompCommand.CONNECT)
    wsSession.simulateConnectedFrameReceived(connectedHeaders)
    val stompSession = session.await()
    Pair(wsSession, stompSession)
}

class ImmediatelySucceedingWebSocketClient(
    private val session: WebSocketSessionMock = WebSocketSessionMock()
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketSession = session
}

class ImmediatelyFailingWebSocketClient(private val exception: Throwable) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketSession = throw exception
}

class ManuallyConnectingWebSocketClient : WebSocketClient {

    private val connectEventChannel = Channel<Unit>()
    private val connectedEventChannel = Channel<WebSocketSession>()

    override suspend fun connect(url: String): WebSocketSession {
        connectEventChannel.send(Unit)
        return connectedEventChannel.receive()
    }

    suspend fun waitForConnectCall() {
        connectEventChannel.receive()
    }

    suspend fun simulateSuccessfulConnection(session: WebSocketSessionMock) {
        connectedEventChannel.send(session)
    }
}
