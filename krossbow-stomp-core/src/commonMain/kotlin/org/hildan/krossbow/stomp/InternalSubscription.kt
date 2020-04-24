package org.hildan.krossbow.stomp

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.hildan.krossbow.stomp.frame.StompFrame

internal class InternalSubscription<out T>(
    override val id: String,
    private val convertMessage: (StompFrame.Message) -> T,
    private val internalSession: BaseStompSession
) : StompSubscription<T> {

    private val internalMsgChannel: Channel<T> =
        Channel()

    override val messages: ReceiveChannel<T> get() = internalMsgChannel

    suspend fun onMessage(message: StompFrame.Message) {
        try {
            internalMsgChannel.send(convertMessage(message))
        } catch (e: Exception) {
            internalMsgChannel.close(MessageConversionException(e))
        }
    }

    fun close(cause: Throwable?) {
        internalMsgChannel.close(cause)
    }

    override suspend fun unsubscribe() {
        internalSession.unsubscribe(id)
        internalMsgChannel.close()
    }
}
