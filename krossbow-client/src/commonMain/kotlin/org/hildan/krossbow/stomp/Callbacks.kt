package org.hildan.krossbow.stomp

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.hildan.krossbow.stomp.headers.StompUnsubscribeHeaders

/**
 * Used to bridge the callback-based platform-specific implementations with the coroutine-based Krossbow sessions.
 */
interface SubscriptionCallbacks<in T> {
    /**
     * Called on each received MESSAGE frame for this subscription.
     */
    suspend fun onReceive(message: KrossbowMessage<T>)

    /**
     * Called if an error occurs in the context of this subscription.
     */
    fun onError(throwable: Throwable)
}

/**
 * An adapter for STOMP subscriptions in a platform-specific engine.
 */
data class KrossbowEngineSubscription(
    /**
     * The subscription ID.
     */
    val id: String,
    /**
     * A function to call in order to unsubscribe from this subscription, optionally taking headers as parameter.
     */
    val unsubscribe: suspend (StompUnsubscribeHeaders?) -> Unit
)

/**
 * Represents a STOMP subscription to receive messages of a single type [T].
 */
class KrossbowSubscription<out T>(
    /**
     * The subscription ID.
     */
    val id: String,
    /**
     * A function to call in order to unsubscribe from this subscription, optionally taking headers as parameter.
     */
    private val internalUnsubscribe: suspend (StompUnsubscribeHeaders?) -> Unit,
    private val internalMsgChannel: Channel<KrossbowMessage<T>>
) {
    /** The subscription messages channel, to read incoming messages from. */
    val messages: ReceiveChannel<KrossbowMessage<T>> get() = internalMsgChannel

    /**
     * Unsubscribes from this subscription to stop receive messages. This closes the [messages] channel, so that any
     * loop on it.
     */
    suspend fun unsubscribe(headers: StompUnsubscribeHeaders? = null) {
        internalUnsubscribe(headers)
        internalMsgChannel.close()
    }

    operator fun component1() = messages
    //    operator fun component2() = this::unsubscribe
}
