package org.hildan.krossbow.stomp

import kotlinx.coroutines.*
import org.hildan.krossbow.stomp.config.*
import org.hildan.krossbow.stomp.version.*
import org.hildan.krossbow.websocket.*
import kotlin.coroutines.*
import kotlin.jvm.*
import kotlin.time.*

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
        webSocketClient: WebSocketClient,
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
     * If some [customStompConnectHeaders] are provided, they are included in the CONNECT (or STOMP) frame and might be
     * used by the server for various usages, e.g. Authorization. Note that this frame is a STOMP-level frame, and is
     * sent after the web socket handshake. The [customStompConnectHeaders] are not sent during the web socket 
     * handshake itself.
     * If you need to sent custom headers in the web socket handshake, use the [WebSocketClient] directly to establish 
     * the web socket connection (with custom headers), and then perform the STOMP handshake as a second step over the 
     * existing web socket connection using [WebSocketConnection.stomp].
     *
     * The `host` header of the CONNECT (or STOMP) frame can be customized via the [host] parameter.
     * This header was introduced as mandatory since STOMP 1.1 and defaults to the host of the provided URL, or is not
     * send in case version 1.0 of the STOMP protocol was negotiated during the web socket handshake as subprotocol.
     * Some old STOMP servers may refuse connections with a `host` header, and not advertise themselves as 1.0 servers
     * during the web socket handshake, in which case you can force it to null to prevent it from being sent.
     *
     * An additional [sessionCoroutineContext] can be provided to override the context used for the collection and
     * decoding of the STOMP frames in the created [StompSession].
     * By default, the session uses [Dispatchers.Default][kotlinx.coroutines.Dispatchers.Default], but it can be
     * overridden here. This is mostly useful to inject a test dispatcher.
     *
     * @throws ConnectionTimeout if this method takes longer than the configured
     * [timeout][StompConfig.connectionTimeout] (as a whole for both WS connect and STOMP connect)
     */
    suspend fun connect(
        url: String,
        login: String? = null,
        passcode: String? = null,
        host: String? = DefaultHost,
        customStompConnectHeaders: Map<String, String> = emptyMap(),
        sessionCoroutineContext: CoroutineContext = EmptyCoroutineContext,
    ): StompSession {
        val session = withTimeoutOrNull(config.connectionTimeout) {
            val webSocket = webSocketClient.connect(
                url = url,
                protocols = StompVersion.preferredOrder.map { it.wsSubprotocolId },
            )
            webSocket.stomp(
                config = config,
                host = host,
                login = login,
                passcode = passcode,
                customHeaders = customStompConnectHeaders,
                sessionCoroutineContext = sessionCoroutineContext,
            )
        }
        return session ?: throw ConnectionTimeout(url, config.connectionTimeout)
    }
}

@Suppress("unused")
@Deprecated(message = "kept only for binary compatibility", level = DeprecationLevel.HIDDEN)
@JvmName("stomp")
suspend fun WebSocketConnection.stompHidden(
    config: StompConfig,
    host: String? = DefaultHost,
    login: String? = null,
    passcode: String? = null,
    customHeaders: Map<String, String> = emptyMap(),
    sessionCoroutineContext: CoroutineContext = EmptyCoroutineContext,
): StompSession = stomp(config, host, login, passcode, customHeaders, sessionCoroutineContext)

/**
 * Exception thrown when the websocket connection + STOMP connection takes too much time.
 */
class ConnectionTimeout(url: String, timeout: Duration) :
    ConnectionException(url, "Timed out waiting for $timeout when connecting to $url")

/**
 * Exception thrown when the connection attempt failed at web socket level.
 */
@Deprecated(
    message = "This exception is no longer thrown by the StompClient, in favor of org.hildan.krossbow.websocket.WebSocketConnectionException.",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("org.hildan.krossbow.websocket.WebSocketConnectionException"),
)
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
