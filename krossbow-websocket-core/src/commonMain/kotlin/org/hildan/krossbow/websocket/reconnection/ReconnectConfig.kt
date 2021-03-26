package org.hildan.krossbow.websocket.reconnection

import org.hildan.krossbow.websocket.WebSocketConnection
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

/**
 * The default value for the maximum number of reconnection attempts before giving up.
 */
internal const val DEFAULT_MAX_ATTEMPTS = 5

/**
 * The default value for the maximum number of reconnection attempts before giving up.
 */
@OptIn(ExperimentalTime::class)
internal val DEFAULT_DELAY_STRATEGY = FixedDelay(1.seconds)

/**
 * Builds a [ReconnectConfig].
 */
fun reconnectConfig(configure: ReconnectConfigBuilder.() -> Unit) = ReconnectConfigBuilder().apply(configure).build()

/**
 * Configuration for web socket reconnections.
 */
data class ReconnectConfig(
    /**
     * The maximum number of reconnection attempts before giving up.
     */
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    /**
     * Defines the time to wait before each reconnection attempt.
     */
    val delayStrategy: RetryDelayStrategy = DEFAULT_DELAY_STRATEGY,
    /**
     * A callback called each time the web socket is successfully reconnected.
     *
     * The [WebSocketConnection] is the same proxy instance after each reconnect, it is just provided for convenience.
     * It is *not* the new underlying connection, which is an implementation detail.
     */
    val afterReconnect: suspend (WebSocketConnection) -> Unit = {},
) {
    init {
        require(maxAttempts >= 0) { "Max number of attempts must not be negative, got $maxAttempts" }
    }
}

class ReconnectConfigBuilder internal constructor() {
    /**
     * The maximum number of reconnection attempts before giving up.
     */
    var maxAttempts: Int = DEFAULT_MAX_ATTEMPTS

    /**
     * Defines the time to wait before each reconnection attempt.
     */
    var delayStrategy: RetryDelayStrategy = DEFAULT_DELAY_STRATEGY

    private var afterReconnect: suspend (WebSocketConnection) -> Unit = {}

    /**
     * A callback called each time the web socket is successfully reconnected.
     *
     * The [WebSocketConnection] parameter is the same proxy instance after each reconnect, it is just provided for
     * convenience. It is *not* the new underlying connection, which is an implementation detail.
     */
    fun afterReconnect(body: suspend (WebSocketConnection) -> Unit) {
        afterReconnect = body
    }

    internal fun build() = ReconnectConfig(maxAttempts, delayStrategy, afterReconnect)
}

