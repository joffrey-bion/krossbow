package org.hildan.krossbow.test

import kotlinx.coroutines.channels.Channel
import org.hildan.krossbow.websocket.KWebSocketClient
import org.hildan.krossbow.websocket.KWebSocketSession

class ImmediatelySucceedingWebSocketClient(
    private val session: WebSocketSessionMock = WebSocketSessionMock()
) : KWebSocketClient {

    override suspend fun connect(url: String): KWebSocketSession = session
}

class ImmediatelyFailingWebSocketClient(private val exception: Throwable) : KWebSocketClient {

    override suspend fun connect(url: String): KWebSocketSession = throw exception
}

class ManuallyConnectingWebSocketClient : KWebSocketClient {

    private val connectEventChannel = Channel<Unit>()
    private val connectedEventChannel = Channel<KWebSocketSession>()

    override suspend fun connect(url: String): KWebSocketSession {
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
