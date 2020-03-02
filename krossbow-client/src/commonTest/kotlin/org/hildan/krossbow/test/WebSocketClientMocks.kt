package org.hildan.krossbow.test

import kotlinx.coroutines.channels.Channel
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketSession

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
