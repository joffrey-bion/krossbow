package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.channels.Channel
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection

/**
 * A mock [WebSocketClient] implementation that allows to control entirely how the [connect] method suspends and
 * resumes. This is useful to test the behavior of the connect mechanism of clients that build on top of existing web
 * socket clients.
 */
class WebSocketClientMock : WebSocketClient {

    override val supportsCustomHeaders: Boolean = false

    private val connectEventChannel = Channel<WebSocketConnectCall>()
    private val connectedEventChannel = Channel<Result<WebSocketConnection>>()

    override suspend fun connect(url: String, protocols: List<String>, headers: Map<String, String>): WebSocketConnection {
        connectEventChannel.send(WebSocketConnectCall(url, protocols, headers))
        return connectedEventChannel.receive().getOrThrow()
    }

    suspend fun awaitConnectCall(): WebSocketConnectCall = connectEventChannel.receive()

    suspend fun simulateSuccessfulConnection(connection: WebSocketConnectionMock) {
        connectedEventChannel.send(Result.success(connection))
    }

    suspend fun simulateFailedConnection(cause: Throwable) {
        connectedEventChannel.send(Result.failure(cause))
    }
    
    suspend fun awaitConnectAndSimulateSuccess(selectedProtocol: String? = null): WebSocketConnectionMock {
        val call = awaitConnectCall()
        val connection = WebSocketConnectionMock(url = call.url, protocol = selectedProtocol)
        simulateSuccessfulConnection(connection)
        return connection
    }
    
    suspend fun awaitConnectAndSimulateFailure(cause: Throwable) {
        awaitConnectCall()
        simulateFailedConnection(cause)
    }
}

/**
 * The parameters of a call to [WebSocketClient.connect].
 */
data class WebSocketConnectCall(
    val url: String,
    val protocols: List<String>,
    val headers: Map<String, String>,
)
