package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection
import org.hildan.krossbow.websocket.default
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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
        webSocketClient: WebSocketClient = WebSocketClient.default(),
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
     * If [customStompConnectHeaders] is provided, its content will be included in the CONNECT (or STOMP) frame and
     * might be used by the server for various usages, e.g. Authorization
     *
     * The `host` header of the CONNECT (or STOMP) frame can be customized via the [host] parameter.
     * This header was introduced as mandatory since STOMP 1.1 and defaults to the host of the provided URL.
     * Some old STOMP servers may refuse connections with a `host` header, in which case you can force it to null to
     * prevent it from being sent.
     *
     * An additional [sessionCoroutineContext] can be provided to override the context used for the collection and
     * decoding of the STOMP frames in the created [StompSession].
     * By default, the session uses [Dispatchers.Default][kotlinx.coroutines.Dispatchers.Default], but it can be
     * overridden here. This is mostly useful to inject a test dispatcher.
     *
     * @throws ConnectionTimeout if this method takes longer than the configured
     * [timeout][StompConfig.connectionTimeoutMillis] (as a whole for both WS connect and STOMP connect)
     */
    suspend fun connect(
        url: String,
        login: String? = null,
        passcode: String? = null,
        host: String? = extractHost(url),
        customStompConnectHeaders: Map<String, String> = emptyMap(),
        sessionCoroutineContext: CoroutineContext = EmptyCoroutineContext,
    ): StompSession {
        val session = withTimeoutOrNull(config.connectionTimeoutMillis) {
            val webSocket = webSocketConnect(url)
            val connectHeaders = StompConnectHeaders(
                host = host,
                login = login,
                passcode = passcode,
                heartBeat = config.heartBeat,
                customHeaders = customStompConnectHeaders
            )
            webSocket.stomp(config, connectHeaders, sessionCoroutineContext)
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
 *
 * If the connection at the STOMP level fails, the underlying web socket is closed.
 */
suspend fun WebSocketConnection.stomp(
    config: StompConfig,
    host: String? = this.host,
    login: String? = null,
    passcode: String? = null,
    customHeaders: Map<String, String> = emptyMap(),
    sessionCoroutineContext: CoroutineContext = EmptyCoroutineContext,
): StompSession {
    val connectHeaders = StompConnectHeaders(
        host = host,
        login = login,
        passcode = passcode,
        heartBeat = config.heartBeat,
        customHeaders = customHeaders,
    )
    return stomp(config, connectHeaders, sessionCoroutineContext)
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
open class WebSocketConnectionException(
    url: String,
    message: String = "Failed to connect at web socket level to $url",
    cause: Throwable? = null,
) : ConnectionException(url, message, cause)

/**
 * Exception thrown when the connection attempt failed at STOMP protocol level.
 */
class StompConnectionException(val host: String?, cause: Throwable? = null) :
    ConnectionException(host ?: "null", "Failed to connect at STOMP protocol level to host '$host'", cause)

/**
 * Exception thrown when something went wrong during the connection.
 */
open class ConnectionException(val url: String, message: String, cause: Throwable? = null) : Exception(message, cause)
