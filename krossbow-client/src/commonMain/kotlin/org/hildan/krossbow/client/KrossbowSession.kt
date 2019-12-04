package org.hildan.krossbow.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.hildan.krossbow.client.converters.MessageConverter
import org.hildan.krossbow.engines.KrossbowEngineSession
import org.hildan.krossbow.engines.KrossbowEngineSubscription
import org.hildan.krossbow.engines.KrossbowMessage
import org.hildan.krossbow.engines.KrossbowReceipt
import org.hildan.krossbow.engines.SubscriptionCallbacks
import org.hildan.krossbow.engines.UnsubscribeHeaders
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * A coroutine-based STOMP session API. The provided [KrossbowEngineSession] is a bridge to the platform implementation.
 */
class KrossbowSession(
    private val engineSession: KrossbowEngineSession,
    private val config: KrossbowConfig
) : CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job

    /**
     * Sends a SEND frame to the server at the given [destination] with the given [payload].
     *
     * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received from the server and returns a
     * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
     * [LostReceiptException] is thrown.
     *
     * If receipts are not enabled, this method sends the frame and immediately returns null.
     */
    suspend inline fun <reified T : Any> send(destination: String, payload: T): KrossbowReceipt? =
            send(destination, payload, T::class)

    /**
     * Sends a SEND frame to the server at the given [destination] with no payload.
     *
     * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received from the server and returns a
     * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
     * [LostReceiptException] is thrown.
     *
     * If receipts are not enabled, this method sends the frame and immediately returns null.
     */
    suspend fun send(destination: String): KrossbowReceipt? = send(destination, null, Any::class)

    /**
     * Sends a SEND frame to the server at the given [destination] with the given [payload].
     *
     * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received from the server and returns a
     * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
     * [LostReceiptException] is thrown.
     *
     * If receipts are not enabled, this method sends the frame and immediately returns null.
     */
    suspend fun <T : Any> send(destination: String, payload: T? = null, payloadType: KClass<T>): KrossbowReceipt? {
        val bytesBody = payload?.let { config.messageConverter.convertToBytes(it, payloadType) }
        return engineSession.send(destination, bytesBody)
    }

    /**
     * Subscribes to the given [destination], expecting objects of type [T]. The returned [KrossbowSubscription]
     * can be used to access the channel of received objects.
     *
     * The configured [MessageConverter] is used to create instances of the given type from the body of every message
     * received on the created subscription. If no payload is received in a message, an exception is thrown, unless
     * [T] is [Unit].
     */
    suspend fun <T : Any> subscribe(destination: String, clazz: KClass<T>): KrossbowSubscription<T> {
        val converter = config.messageConverter
        requireNotNull(converter) {
            "No MessageConverter was configured, unable to handle arbitrary subscription types"
        }
        val channel = Channel<KrossbowMessage<T>>()
        val callbacks = CallbacksAdapter(converter, clazz, channel)
        val sub = engineSession.subscribe(destination, callbacks)
        return KrossbowSubscription(sub, channel)
    }

    /**
     * Subscribes to the given [destination], expecting empty payloads.
     */
    suspend fun subscribeNoPayload(destination: String): KrossbowSubscription<Unit> {
        val channel = Channel<KrossbowMessage<Unit>>()
        val sub = engineSession.subscribeNoPayload(destination, EmptyPayloadCallbacks(channel))
        return KrossbowSubscription(sub, channel)
    }

    /**
     * Subscribes to the given [destination], expecting objects of type [T]. The returned [KrossbowSubscription]
     * can be used to access the channel of received objects.
     *
     * A platform-specific deserializer is used to create instances of the given type from the body of every message
     * received on the created subscription.
     */
    suspend inline fun <reified T : Any> KrossbowSession.subscribe(destination: String): KrossbowSubscription<T> =
            subscribe(destination, T::class)

    /**
     * Sends a DISCONNECT frame to close the session, and closes the connection.
     */
    suspend fun disconnect() {
        job.cancelAndJoin()
        engineSession.disconnect()
    }
}

private class CallbacksAdapter<T : Any>(
    private val messageConverter: MessageConverter,
    private val clazz: KClass<T>,
    private val channel: SendChannel<KrossbowMessage<T>>
) : SubscriptionCallbacks<ByteArray> {

    override suspend fun onReceive(message: KrossbowMessage<ByteArray>) {
        channel.send(messageConverter.convertFromBytes(message, clazz))
    }

    override fun onError(throwable: Throwable) {
        channel.close(throwable)
    }
}

private class EmptyPayloadCallbacks(
    private val channel: SendChannel<KrossbowMessage<Unit>>
) : SubscriptionCallbacks<Unit> {

    override suspend fun onReceive(message: KrossbowMessage<Unit>) {
        channel.send(message)
    }

    override fun onError(throwable: Throwable) {
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
