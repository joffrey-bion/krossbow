package org.hildan.krossbow.engines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

// TODO handle headers in params
interface KrossbowSession: CoroutineScope {

    suspend fun send(destination: String, body: Any): KrossbowReceipt?

    suspend fun <T : Any> subscribe(destination: String, clazz: KClass<T>): KrossbowSubscription<T>

    suspend fun disconnect()
}

suspend inline fun <reified T : Any> KrossbowSession.subscribe(destination: String): KrossbowSubscription<T> =
    subscribe(destination, T::class)

class AbstractKrossbowSession(private val engineSession: KrossbowEngineSession): KrossbowSession {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job

    override suspend fun send(destination: String, body: Any): KrossbowReceipt? = engineSession.send(destination, body)

    override suspend fun <T : Any> subscribe(destination: String, clazz: KClass<T>): KrossbowSubscription<T> {
        val channel = Channel<KrossbowMessage<T>>()
        val sub = engineSession.subscribe(destination, clazz, SubscriptionCallbacks(channel))
        return KrossbowSubscription(sub, channel)
    }

    override suspend fun disconnect() {
        job.cancelAndJoin()
        engineSession.disconnect()
    }
}

class SubscriptionCallbacks<in T>(private val channel: SendChannel<KrossbowMessage<T>>) {

    suspend fun onReceive(message: KrossbowMessage<T>) {
        channel.send(message)
    }

    fun onError(throwable: Throwable) {
        channel.close(throwable)
    }
}

class KrossbowSubscription<out T>(
    private val engineSubscription: KrossbowEngineSubscription,
    private val internalMsgChannel: Channel<KrossbowMessage<T>>
) {
    val id: String = engineSubscription.id
    val messages: ReceiveChannel<KrossbowMessage<T>> get() = internalMsgChannel

    suspend fun unsubscribe(headers: UnsubscribeHeaders? = null) {
        engineSubscription.unsubscribe(headers)
        internalMsgChannel.close()
    }

    operator fun component0() = messages
    operator fun component1() = ::unsubscribe
}
