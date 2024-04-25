package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.channels.Channel
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection

fun webSocketClientMock(connect: suspend () -> WebSocketConnection = { WebSocketConnectionMock() }) =
    object : WebSocketClient {

        override val supportsCustomHeaders: Boolean = false

        override suspend fun connect(url: String, protocols: List<String>, headers: Map<String, String>): WebSocketConnection = connect()
    }

/**
 * A mock [WebSocketClient] implementation that allows to control entirely how the [connect] method suspends and
 * resumes. This is useful to test the behaviour of the connect mechanism of clients that build on top of existing web
 * socket clients.
 */
class ControlledWebSocketClientMock : WebSocketClient {

    override val supportsCustomHeaders: Boolean = false

    private val connectEventChannel = Channel<Unit>()
    private val connectedEventChannel = Channel<Result<WebSocketConnection>>()

    override suspend fun connect(url: String, protocols: List<String>, headers: Map<String, String>): WebSocketConnection {
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
