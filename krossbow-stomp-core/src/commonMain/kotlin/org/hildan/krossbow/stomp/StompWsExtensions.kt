package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.stomp.heartbeats.negotiated
import org.hildan.krossbow.websocket.WebSocketConnection
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class) // FIXME this is for withTimeoutOrNull(Duration), remove with coroutines 1.6.0
internal suspend fun WebSocketConnection.stomp(
    config: StompConfig,
    headers: StompConnectHeaders,
    sessionCoroutineContext: CoroutineContext,
): StompSession {
    val stompSocket = StompSocket(this, config)
    try {
        val connectedFrame = withTimeoutOrNull(config.connectionTimeout) {
            stompSocket.connectHandshake(headers, config.connectWithStompCommand)
        }
        if (connectedFrame == null) {
            val connectionTimeout = ConnectionTimeout(headers.host ?: "null", config.connectionTimeout)
            stompSocket.close(connectionTimeout)
            throw connectionTimeout
        }
        val negotiatedHeartBeat = config.heartBeat.negotiated(connectedFrame.headers.heartBeat)
        val contextOverrides = config.defaultSessionCoroutineContext + sessionCoroutineContext
        return BaseStompSession(config, stompSocket, negotiatedHeartBeat, contextOverrides)
    } catch (e: CancellationException) {
        withContext(NonCancellable) {
            stompSocket.close(e)
        }
        // this cancellation comes from the outside, we should not wrap this exception
        throw e
    } catch (e: Exception) {
        throw StompConnectionException(headers.host, cause = e)
    }
}

private suspend fun StompSocket.connectHandshake(
    headers: StompConnectHeaders,
    connectWithStompCommand: Boolean,
): StompFrame.Connected = coroutineScope {
    val futureConnectedFrame = async(start = CoroutineStart.UNDISPATCHED) {
        awaitConnectedFrame()
    }
    val connectFrame = if (connectWithStompCommand) {
        StompFrame.Stomp(headers)
    } else {
        StompFrame.Connect(headers)
    }
    sendStompFrame(connectFrame)
    futureConnectedFrame.await()
}

private suspend fun StompSocket.awaitConnectedFrame(): StompFrame.Connected {
    val stompEvent = incomingEvents.first()
    check(stompEvent is StompFrame.Connected) { "Expected CONNECTED frame in response to CONNECT, got $stompEvent" }
    return stompEvent
}
