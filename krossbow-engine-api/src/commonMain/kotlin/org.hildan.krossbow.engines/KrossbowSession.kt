package org.hildan.krossbow.engines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * A coroutine-based STOMP session API. The provided [KrossbowEngineSession] is a bridge to the platform implementation.
 */
class KrossbowSession(private val engineSession: KrossbowEngineSession) : CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job

    /**
     * Sends a SEND frame to the server at the given [destination] with the given [body].
     *
     * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received form the server and returns a
     * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
     * [LostReceiptException] is thrown.
     *
     * If receipts are not enabled, this method sends the frame and immediately returns null.
     */
    suspend fun send(destination: String, body: Any? = null): KrossbowReceipt? = engineSession.send(destination, body)

    /**
     * Subscribes to the given [destination], expecting objects of type [T]. The returned [KrossbowSubscription]
     * can be used to access the channel of received objects.
     *
     * A platform-specific deserializer is used to create instances of the given type from the body of every message
     * received on the created subscription.
     */
    suspend inline fun <reified T : Any> subscribe(destination: String): KrossbowSubscription<T> =
            subscribe(destination, T::class)

    /**
     * Subscribes to the given [destination], expecting objects of type [clazz]. The returned [KrossbowSubscription]
     * can be used to access the channel of received objects.
     *
     * A platform-specific deserializer is used to create instances of the given type from the body of every message
     * received on the created subscription.
     */
    suspend fun <T : Any> subscribe(destination: String, clazz: KClass<T>): KrossbowSubscription<T> {
        val channel = Channel<KrossbowMessage<T>>()
        val sub = engineSession.subscribe(destination, clazz, SubscriptionCallbacks(channel))
        return KrossbowSubscription(sub, channel)
    }

    /**
     * Subscribes to the given [destination], expecting empty payloads.
     */
    suspend fun subscribeNoPayload(destination: String): KrossbowSubscription<Unit> {
        val channel = Channel<KrossbowMessage<Unit>>()
        val sub = engineSession.subscribeNoPayload(destination, SubscriptionCallbacks(channel))
        return KrossbowSubscription(sub, channel)
    }

    /**
     * Sends a DISCONNECT frame to close the session, and closes the connection.
     */
    suspend fun disconnect() {
        job.cancelAndJoin()
        engineSession.disconnect()
    }
}

/**
 * Used to bridge the callback-based platform-specific implementations with the coroutine-based [KrossbowSession]
 */
class SubscriptionCallbacks<in T>(private val channel: SendChannel<KrossbowMessage<T>>) {

    suspend fun onReceive(message: KrossbowMessage<T>) {
        channel.send(message)
    }

    fun onError(throwable: Throwable) {
        channel.close(throwable)
    }
}

/**
 * Represents a STOMP subscription to receive messages of a single type [T].
 */
class KrossbowSubscription<out T>(
    private val engineSubscription: KrossbowEngineSubscription,
    private val internalMsgChannel: Channel<KrossbowMessage<T>>
) {
    /** The subscription's ID. */
    val id: String = engineSubscription.id

    /** The subscription messages channel, to read incoming messages from. */
    val messages: ReceiveChannel<KrossbowMessage<T>> get() = internalMsgChannel

    /**
     * Unsubscribes from this subscription to stop receive messages. This closes the [messages] channel, so that any
     * loop on it.
     */
    suspend fun unsubscribe(headers: UnsubscribeHeaders? = null) {
        engineSubscription.unsubscribe(headers)
        internalMsgChannel.close()
    }

    operator fun component1() = messages
//    operator fun component2() = this::unsubscribe
}
