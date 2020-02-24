package org.hildan.krossbow.stomp

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.headers.StompConnectHeaders
import org.hildan.krossbow.websocket.KWebSocketClient
import org.hildan.krossbow.websocket.KWebSocketSession
import org.hildan.krossbow.websocket.defaultSockJSClient
import org.hildan.krossbow.websocket.defaultWebSocketClient

/**
 * A STOMP client based on the given web socket implementation.
 * The client is used to connect to the server and create a [StompSession].
 * Then, most of the STOMP interactions are done through the [StompSession].
 */
class StompClient(
    private val webSocketClient: KWebSocketClient,
    private val config: StompConfig
) {
    constructor(
        webSocketClient: KWebSocketClient = defaultWebSocketClient(),
        configure: StompConfig.() -> Unit = {}
    ) : this(
        webSocketClient = webSocketClient,
        config = StompConfig().apply { configure() }
    )

    /**
     * Connects to the given WebSocket [url] and to the STOMP session, and returns after receiving the CONNECTED frame.
     */
    suspend fun connect(url: String, login: String? = null, passcode: String? = null): StompSession {
        try {
            return withTimeout(config.connectionTimeoutMillis) {
                val wsSession = webSocketClient.connect(url)
                wsSession.stompConnect(url, login, passcode)
            }
        } catch (te: TimeoutCancellationException) {
            throw ConnectionTimeout(
                "Timeout of ${config.connectionTimeoutMillis}ms exceeded when connecting to $url",
                te
            )
        } catch (e: Exception) {
            throw ConnectionException("Couldn't connect to STOMP server at $url", e)
        }
    }

    private suspend fun KWebSocketSession.stompConnect(url: String, login: String?, passcode: String?): StompSession {
        val host = extractHost(url)
        val connectHeaders = StompConnectHeaders(
            host = host,
            login = login,
            passcode = passcode,
            heartBeat = config.heartBeat
        )
        val stompSession = InternalStompSession(config, this)
        stompSession.connect(connectHeaders)
        return stompSession
    }

    private fun extractHost(url: String) = url.substringAfter("://").substringBefore("/").substringBefore(":")

    companion object {

        fun withSockJS(configure: StompConfig.() -> Unit) = StompClient(defaultSockJSClient(), configure)
    }
}

/**
 * An exception thrown when something went wrong during the connection.
 */
class ConnectionException(message: String, cause: Throwable) : Exception(message, cause)

/**
 * Connects to the given [url] and executes the given [block] with the created session. The session is then
 * automatically closed at the end of the block.
 */
suspend fun StompClient.useSession(
    url: String,
    login: String? = null,
    passcode: String? = null,
    block: suspend StompSession.() -> Unit
) {
    val session = connect(url, login, passcode)
    try {
        session.block()
    } finally {
        session.disconnect()
    }
}
