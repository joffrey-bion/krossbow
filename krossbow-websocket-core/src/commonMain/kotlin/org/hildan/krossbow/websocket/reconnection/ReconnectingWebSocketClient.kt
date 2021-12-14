package org.hildan.krossbow.websocket.reconnection

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection
import org.hildan.krossbow.websocket.WebSocketConnectionException
import org.hildan.krossbow.websocket.WebSocketFrame
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
fun WebSocketClient.withAutoReconnect(config: ReconnectConfig): WebSocketClient = when (this) {
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
fun WebSocketClient.withAutoReconnect(configure: ReconnectConfigBuilder.() -> Unit): WebSocketClient =
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
): WebSocketClient = withAutoReconnect {
    this.maxAttempts = maxAttempts
    this.delayStrategy = delayStrategy
}

private class ReconnectingWebSocketClient(
    val baseClient: WebSocketClient,
    private val reconnectConfig: ReconnectConfig,
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketConnection {
        val firstConnection = baseClient.connect(url)
        return WebSocketConnectionProxy(baseClient, reconnectConfig, firstConnection)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class WebSocketConnectionProxy(
    private val baseClient: WebSocketClient,
    private val reconnectConfig: ReconnectConfig,
    private var currentConnection: WebSocketConnection,
) : WebSocketConnection {

    private val scope = CoroutineScope(CoroutineName("krossbow-reconnection-watcher"))

    override val url: String
        get() = currentConnection.url
    override val canSend: Boolean
        get() = currentConnection.canSend

    private val _frames: Channel<WebSocketFrame> = Channel()
    override val incomingFrames: Flow<WebSocketFrame> = _frames.receiveAsFlow()

    init {
        scope.launch {
            while (isActive) {
                try {
                    currentConnection.incomingFrames.collect {
                        _frames.send(it)
                    }
                    _frames.close()
                    break
                } catch (e: CancellationException) {
                    throw e // let cancellation through
                } catch (e: Exception) {
                    try {
                        currentConnection = reconnect(e)
                    } catch (e: CancellationException) {
                        throw e // let cancellation through
                    } catch (e: Exception) {
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
            if (!reconnectConfig.shouldReconnect(lastAttemptException, attempt)) {
                throw lastAttemptException
            }
            try {
                delay(reconnectConfig.delayStrategy.computeDelay(attempt))
                return baseClient.connect(currentConnection.url).also {
                    reconnectConfig.afterReconnect(this)
                }
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
    message: String = "Could not reconnect to web socket at $url after $nAttemptedReconnections attempts. Giving up. " +
        "The exception during the last attempt was $cause",
) : WebSocketConnectionException(url, message, cause)
