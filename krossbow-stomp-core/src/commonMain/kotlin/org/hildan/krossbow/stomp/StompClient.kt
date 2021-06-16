package org.hildan.krossbow.stomp

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection
import org.hildan.krossbow.websocket.defaultWebSocketClient
import kotlin.coroutines.coroutineContext

/**
 * A STOMP 1.2 client based on web sockets.
 * A custom web socket implementation can be passed in as a constructor parameter.
 *
 * The client is used to connect to the server and create a [StompSession].
 * Then, most of the STOMP interactions are done through the [StompSession] until it is
 * [disconnected][StompSession.disconnect].
 *
 * The same client can be reused to start multiple sessions in general, unless a particular limitation from the
 * underlying web socket implementation prevents this.
 */
class StompClient(
    private val webSocketClient: WebSocketClient,
    private val config: StompConfig,
) {
    constructor(
        webSocketClient: WebSocketClient = defaultWebSocketClient(),
        configure: StompConfig.() -> Unit = {},
    ) : this(
        webSocketClient = webSocketClient,
        config = StompConfig().apply { configure() },
    )

    /**
     * Connects to the given WebSocket [url] and to the STOMP session, and returns after receiving the CONNECTED frame.
     *
     * If [login] and [passcode] are provided, they are used to connect at STOMP level (after the web socket
     * connection is established).
     *
     * If [customStompConnectHeaders] is provided, its content will be included in the CONNECT/STOMP frame and
     * might be used by the server for various usages, e.g. Authorization
     *
     * @throws ConnectionTimeout if this method takes longer than the configured
     * [timeout][StompConfig.connectionTimeoutMillis] (as a whole for both WS connect and STOMP connect)
     */
    suspend fun connect(
        url: String,
        login: String? = null,
        passcode: String? = null,
        host: String = extractHost(url),
        customStompConnectHeaders: Map<String, String> = emptyMap(),
    ): StompSession {
        val session = withTimeoutOrNull(config.connectionTimeoutMillis) {
            val wsSession = webSocketConnect(url)
            val connectHeaders = StompConnectHeaders(
                host = host,
                login = login,
                passcode = passcode,
                heartBeat = config.heartBeat,
                customHeaders = customStompConnectHeaders
            )
            wsSession.stomp(config, connectHeaders)
        }
        return session ?: throw ConnectionTimeout(url, config.connectionTimeoutMillis)
    }

    private suspend fun webSocketConnect(url: String): WebSocketConnection {
        try {
            return webSocketClient.connect(url)
        } catch (e: CancellationException) {
            // this cancellation comes from the outside, we should not wrap this exception
            throw e
        } catch (e: Exception) {
            throw WebSocketConnectionException(url, cause = e)
        }
    }
}

/**
 * Establishes a STOMP session over an existing [WebSocketConnection].
 *
 * The behaviour of the STOMP protocol can be customized via the [config].
 * However, the semantics of [StompConfig.connectionTimeoutMillis] is slightly changed: it doesn't take into account
 * the web socket connection time (since it already happened outside of this method call).
 *
 * If [login] and [passcode] are provided, they are used for STOMP authentication.
 *
 * The CONNECT/STOMP frame can be further customized by using [customHeaders], which may be useful for server-specific
 * behaviour, like token-based authentication.
 */
suspend fun WebSocketConnection.stomp(
    config: StompConfig,
    host: String = this.host,
    login: String? = null,
    passcode: String? = null,
    customHeaders: Map<String, String> = emptyMap()
): StompSession {
    val session = withTimeoutOrNull(config.connectionTimeoutMillis) {
        val connectHeaders = StompConnectHeaders(
            host = host,
            login = login,
            passcode = passcode,
            heartBeat = config.heartBeat,
            customHeaders = customHeaders,
        )
        stomp(config, connectHeaders)
    }
    return session ?: throw ConnectionTimeout(host, config.connectionTimeoutMillis)
}

internal suspend fun WebSocketConnection.stomp(config: StompConfig, headers: StompConnectHeaders): StompSession {
    try {
        val stompSession = BaseStompSession(config, StompSocket(this, config, coroutineContext))
        stompSession.connect(headers)
        return stompSession
    } catch (e: CancellationException) {
        // this cancellation comes from the outside, we should not wrap this exception
        throw e
    } catch (e: Exception) {
        throw StompConnectionException(headers.host, cause = e)
    }
}

private fun extractHost(url: String) = url.substringAfter("://").substringBefore("/").substringBefore(":")

/**
 * Exception thrown when the websocket connection + STOMP connection takes too much time.
 */
class ConnectionTimeout(url: String, timeoutMillis: Long) :
    ConnectionException(url, "Timed out waiting for ${timeoutMillis}ms when connecting to $url")

/**
 * Exception thrown when the connection attempt failed at web socket level.
 */
class WebSocketConnectionException(url: String, cause: Throwable? = null) :
    ConnectionException(url, "Failed to connect at web socket level to $url", cause)

/**
 * Exception thrown when the connection attempt failed at STOMP protocol level.
 */
class StompConnectionException(host: String, cause: Throwable? = null) :
    ConnectionException(host, "Failed to connect at STOMP protocol level to $host", cause)

/**
 * Exception thrown when something went wrong during the connection.
 */
open class ConnectionException(val url: String, message: String, cause: Throwable? = null) : Exception(message, cause)
