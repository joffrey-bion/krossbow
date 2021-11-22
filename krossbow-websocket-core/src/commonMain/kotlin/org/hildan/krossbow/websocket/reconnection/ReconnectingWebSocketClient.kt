package org.hildan.krossbow.websocket.reconnection

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.hildan.krossbow.websocket.*
import kotlin.time.ExperimentalTime

/**
 * Returns a new [WebSocketClient] that automatically reconnects on web socket errors using this client.
 *
 * The [WebSocketConnection] returned by [connect][WebSocketClient.connect] is an abstraction over this client's
 * connections, so that the same connection instance can be used across reconnections, which happen transparently
 * under the hood.
 *
 * When chaining multiple [withAutoReconnect] calls, the last reconnect configuration takes precedence.
 *
 * Note: limitations on Kotlin/Native multithreaded coroutines prevent the reconnection wrapper from working properly.
 * Please use the new memory model if you want the reconnection feature on Kotlin/Native.
 */
fun WebSocketClient.withAutoReconnect(config: ReconnectConfig): ReconnectingWebSocketClient = when (this) {
    is ReconnectingWebSocketClient -> ReconnectingWebSocketClient(baseClient, config)
    else -> ReconnectingWebSocketClient(this, config)
}

/**
 * Returns a new [WebSocketClient] that automatically reconnects on web socket errors using this client.
 *
 * The [WebSocketConnection] returned by [connect][WebSocketClient.connect] is an abstraction over this client's
 * connections, so that the same connection instance can be used across reconnections, which happen transparently
 * under the hood.
 *
 * When chaining multiple [withAutoReconnect] calls, the last reconnect configuration takes precedence.
 *
 * Note: limitations on Kotlin/Native multithreaded coroutines prevent the reconnection wrapper from working properly.
 * Please use the new memory model if you want the reconnection feature on Kotlin/Native.
 */
fun WebSocketClient.withAutoReconnect(configure: ReconnectConfigBuilder.() -> Unit): ReconnectingWebSocketClient =
    withAutoReconnect(reconnectConfig(configure))

/**
 * Returns a new [WebSocketClient] that automatically reconnects on web socket errors using this client.
 *
 * The [WebSocketConnection] returned by [connect][WebSocketClient.connect] is an abstraction over this client's
 * connections, so that the same connection instance can be used across reconnections, which happen transparently
 * under the hood.
 *
 * When chaining multiple [withAutoReconnect] calls, the last reconnect configuration takes precedence.
 *
 * Note: limitations on Kotlin/Native multithreaded coroutines prevent the reconnection wrapper from working properly.
 * Please use the new memory model if you want the reconnection feature on Kotlin/Native.
 */
fun WebSocketClient.withAutoReconnect(
    maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    delayStrategy: RetryDelayStrategy = DEFAULT_DELAY_STRATEGY,
): ReconnectingWebSocketClient = withAutoReconnect {
    this.maxAttempts = maxAttempts
    this.delayStrategy = delayStrategy
}

class ReconnectingWebSocketClient internal constructor(
    internal val baseClient: WebSocketClient,
    private val reconnectConfig: ReconnectConfig,
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketConnection {
        val firstConnection = baseClient.connect(url)
        return WebSocketConnectionProxy(baseClient, reconnectConfig, firstConnection)
    }

    suspend fun connect(url: String, afterReconnect: (WebSocketConnection) -> Unit): WebSocketConnection {
        val firstConnection = baseClient.connect(url)
        val updatedConfig = reconnectConfig.copy(afterReconnect = {
            reconnectConfig.afterReconnect(it)
            afterReconnect(it)
        })
        return WebSocketConnectionProxy(baseClient, updatedConfig, firstConnection)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketConnectionProxy internal constructor(
    private val baseClient: WebSocketClient,
    private val reconnectConfig: ReconnectConfig,
    private var currentConnection: WebSocketConnection,
) : WebSocketConnection {

    private val scope = CoroutineScope(CoroutineName("krossbow-reconnection-watcher"))

    override val url: String
        get() = currentConnection.url
    override val canSend: Boolean
        get() = currentConnection.canSend

    private val _frames: Channel<WebSocketEvent> = Channel()
    override val incomingFrames: Flow<WebSocketEvent> = _frames.receiveAsFlow()

    init {
        scope.launch {
            while (isActive) {
                try {
                    currentConnection.incomingFrames.collect {
                        _frames.send(it)
                    }
                } catch (e: CancellationException) {
                    throw e // let cancellation through
                } catch (e: Exception) {
                    try {
                        currentConnection = reconnect(e)
                        reconnectConfig.afterReconnect(this@WebSocketConnectionProxy)
                        _frames.send(WebSocketReconnected)
                    } catch (e: WebSocketReconnectionException) {
                        _frames.close(e)
                        break
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun reconnect(cause: Exception): WebSocketConnection {
        var lastAttemptException: Exception = cause
        repeat(reconnectConfig.maxAttempts) { attempt ->
            try {
                delay(reconnectConfig.delayStrategy.computeDelay(attempt))
                return baseClient.connect(currentConnection.url)
            } catch (e: CancellationException) {
                throw e // let cancellation through
            } catch (e: Exception) {
                lastAttemptException = e
            }
        }
        throw WebSocketReconnectionException(currentConnection.url, reconnectConfig.maxAttempts, lastAttemptException)
    }

    override suspend fun sendText(frameText: String) {
        currentConnection.sendText(frameText)
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        currentConnection.sendBinary(frameData)
    }

    override suspend fun close(code: Int, reason: String?) {
        currentConnection.close(code, reason)
        scope.cancel()
    }
}

class WebSocketReconnectionException(
    url: String,
    val nAttemptedReconnections: Int,
    cause: Exception,
    message: String = "Could not reconnect to web socket at $url after $nAttemptedReconnections attempts. Giving up.",
) : WebSocketConnectionException(url, message, cause)
