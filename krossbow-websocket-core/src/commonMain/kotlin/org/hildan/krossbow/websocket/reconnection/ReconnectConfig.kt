package org.hildan.krossbow.websocket.reconnection

import org.hildan.krossbow.websocket.WebSocketConnection
import kotlin.coroutines.*
import kotlin.time.Duration.Companion.seconds

/**
 * The default value for the maximum number of reconnection attempts before giving up.
 */
internal const val DEFAULT_MAX_ATTEMPTS = 5

/**
 * The default value for the reconnection delay strategy.
 */
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
     * An additional coroutine context for the coroutine that handles reconnections.
     */
    val coroutineContext: CoroutineContext = EmptyCoroutineContext,
    /**
     * A predicate to decide whether the web socket should be reconnected when the given `exception` occur.
     * The `attempt` parameter is the index of the current reconnection attempt in a series of retries.
     *
     * When the web socket throws an exception, this predicate is called with attempt 0 before trying to reconnect.
     * If the predicate returns false, the exception is rethrown and no reconnection is attempted.
     * If the predicate returns true, a reconnection is attempted.
     *
     * If the reconnection fails, the predicate is called again with attempt 1, and so on.
     * If the reconnection succeeds, and later a new error occurs on the web socket, the predicate will be called
     * again, with attempt 0.
     *
     * The predicate will not be called if [maxAttempts] is reached. If you want to control the maximum attempts via
     * the predicate, set [maxAttempts] to a bigger value (such as [Int.MAX_VALUE]).
     */
    val shouldReconnect: suspend (exception: Throwable, attempt: Int) -> Boolean = { _, _ -> true },
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

    /**
     * An additional coroutine context for the coroutine that handles reconnections.
     */
    var coroutineContext: CoroutineContext = EmptyCoroutineContext

    private var shouldReconnect: suspend (Throwable, Int) -> Boolean = { _, _ -> true }

    private var afterReconnect: suspend (WebSocketConnection) -> Unit = {}

    /**
     * Registers a [predicate] to decide whether the web socket should be reconnected when the given `exception` occur.
     * The `attempt` parameter is the index of the current reconnection attempt in a series of retries.
     *
     * When the web socket throws an exception, this predicate is called with attempt 0 before trying to reconnect.
     * If the predicate returns false, the exception is rethrown and no reconnection is attempted.
     * If the predicate returns true, a reconnection is attempted.
     *
     * If the reconnection fails, the predicate is called again with attempt 1, and so on.
     * If the reconnection succeeds, and later a new error occurs on the web socket, the predicate will be called
     * again, with attempt 0.
     *
     * The predicate will not be called if [maxAttempts] is reached. If you want to control the maximum attempts via
     * the predicate, set [maxAttempts] to a bigger value (such as [Int.MAX_VALUE]).
     */
    fun reconnectWhen(predicate: suspend (exception: Throwable, attempt: Int) -> Boolean) {
        shouldReconnect = predicate
    }

    /**
     * A callback called each time the web socket is successfully reconnected.
     *
     * The [WebSocketConnection] parameter is the same proxy instance after each reconnect, it is just provided for
     * convenience. It is *not* the new underlying connection, which is an implementation detail.
     */
    fun afterReconnect(body: suspend (WebSocketConnection) -> Unit) {
        afterReconnect = body
    }

    internal fun build() = ReconnectConfig(maxAttempts, delayStrategy, coroutineContext, shouldReconnect, afterReconnect)
}

