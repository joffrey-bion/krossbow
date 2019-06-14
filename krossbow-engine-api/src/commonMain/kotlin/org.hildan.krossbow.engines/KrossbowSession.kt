package org.hildan.krossbow.engines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

class KrossbowSession(private val engineSession: KrossbowEngineSession): CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job

    suspend fun send(destination: String, body: Any): KrossbowReceipt? = engineSession.send(destination, body)

    suspend fun <T : Any> subscribe(destination: String, clazz: KClass<T>): KrossbowSubscription<T> {
        val channel = Channel<KrossbowMessage<T>>()
        val sub = engineSession.subscribe(destination, clazz, SubscriptionCallbacks(channel))
        return KrossbowSubscription(sub, channel)
    }

    suspend inline fun <reified T : Any> subscribe(destination: String): KrossbowSubscription<T> =
        subscribe(destination, T::class)

    suspend fun disconnect() {
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

    operator fun component1() = messages
    operator fun component2() = ::unsubscribe
}
