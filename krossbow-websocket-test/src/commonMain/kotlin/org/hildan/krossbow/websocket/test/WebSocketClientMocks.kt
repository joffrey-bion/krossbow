package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.channels.Channel
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection

fun webSocketClientMock(connect: suspend () -> WebSocketConnection = { WebSocketConnectionMock() }) =
    object : WebSocketClient {
        override suspend fun connect(url: String): WebSocketConnection = connect()
    }

class ControlledWebSocketClientMock : WebSocketClient {

    private val connectEventChannel = Channel<Unit>()
    private val connectedEventChannel = Channel<Result<WebSocketConnection>>()

    override suspend fun connect(url: String): WebSocketConnection {
        connectEventChannel.send(Unit)
        return connectedEventChannel.receive().getOrThrow()
    }

    suspend fun waitForConnectCall() {
        connectEventChannel.receive()
    }

    suspend fun simulateSuccessfulConnection(connection: WebSocketConnectionMock) {
        connectedEventChannel.send(Result.success(connection))
    }

    suspend fun simulateFailedConnection(cause: Throwable) {
        connectedEventChannel.send(Result.failure(cause))
    }
}
