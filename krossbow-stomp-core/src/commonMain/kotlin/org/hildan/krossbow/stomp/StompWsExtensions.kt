package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.hildan.krossbow.stomp.config.*
import org.hildan.krossbow.stomp.frame.*
import org.hildan.krossbow.stomp.headers.*
import org.hildan.krossbow.stomp.heartbeats.*
import org.hildan.krossbow.stomp.version.*
import org.hildan.krossbow.websocket.*
import kotlin.coroutines.*

/**
 * A constant representing the default value for the host header. It is replaced by the real default header value
 * when actually connecting. This constant is necessary because we want to allow `null` as a user-provided value (which
 * should be distinguishable from the default).
 */
internal const val DefaultHost = "<default host header>" // invalid host value to prevent conflicts with real hosts 

/**
 * Establishes a STOMP session over an existing [WebSocketConnection].
 *
 * The behaviour of the STOMP protocol can be customized via the [config].
 * However, the semantics of [StompConfig.connectionTimeout] is slightly changed: it doesn't take into account
 * the web socket connection time (since it already happened outside of this method call).
 *
 * If [login] and [passcode] are provided, they are used for STOMP authentication.
 *
 * The CONNECT/STOMP frame can be further customized by using [customHeaders], which may be useful for server-specific
 * behaviour, like token-based authentication.
 *
 * If the connection at the STOMP level fails, the underlying web socket is closed.
 */
suspend fun WebSocketConnection.stomp(
    config: StompConfig,
    host: String? = DefaultHost,
    login: String? = null,
    passcode: String? = null,
    customHeaders: Map<String, String> = emptyMap(),
    sessionCoroutineContext: CoroutineContext = EmptyCoroutineContext,
): StompSession {
    val wsStompVersion = StompVersion.fromWsProtocol(protocol)
    val serverPossiblySupportsHost = wsStompVersion == null || wsStompVersion.supportsHostHeader
    val connectHeaders = StompConnectHeaders(
        host = if (host == DefaultHost) this.host.takeIf { serverPossiblySupportsHost } else host,
        acceptVersion = StompVersion.preferredOrder.map { it.headerValue },
        login = login,
        passcode = passcode,
        heartBeat = config.heartBeat,
        customHeaders = customHeaders,
    )
    return stomp(config, connectHeaders, sessionCoroutineContext)
}

private suspend fun WebSocketConnection.stomp(
    config: StompConfig,
    headers: StompConnectHeaders,
    sessionCoroutineContext: CoroutineContext,
): StompSession {
    val stompSocket = StompSocket(this, config)
    try {
        val connectedFrame = withTimeoutOrNull(config.connectionTimeout) {
            stompSocket.connectHandshake(headers, config.connectWithStompCommand)
        } ?: throw ConnectionTimeout(headers.host ?: "null", config.connectionTimeout)

        if (config.failOnStompVersionMismatch) {
            val wsStompVersion = StompVersion.fromWsProtocol(protocol)
            val realStompVersion = StompVersion.fromConnectedFrame(connectedFrame.headers.version)
            check(wsStompVersion == null || wsStompVersion == realStompVersion) {
                "negotiated STOMP version mismatch: " +
                    "$wsStompVersion at web socket level (subprotocol '$protocol'), " +
                    "$realStompVersion at STOMP level"
            }
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
    } catch (e: ConnectionTimeout) {
        stompSocket.close(e)
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
