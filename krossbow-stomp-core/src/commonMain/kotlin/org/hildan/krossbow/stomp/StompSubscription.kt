package org.hildan.krossbow.stomp

import kotlinx.coroutines.channels.ReceiveChannel

/**
 * A subscription to a STOMP destination, streaming messages of type [T].
 */
interface StompSubscription<out T> {
    /**
     * The subscription ID used by the STOMP protocol.
     */
    val id: String

    /**
     * The subscription messages channel, to read incoming messages from.
     */
    val messages: ReceiveChannel<T>

    /**
     * Unsubscribes from this subscription to stop receive messages. This closes the [messages] channel, so that any
     * loop on it stops as well.
     */
    suspend fun unsubscribe()
}
