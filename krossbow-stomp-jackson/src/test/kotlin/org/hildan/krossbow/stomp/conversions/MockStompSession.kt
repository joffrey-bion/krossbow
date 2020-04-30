package org.hildan.krossbow.stomp.conversions

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.StompSubscription
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompMessageHeaders
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders

class MockStompSession : StompSession {

    private val incomingFrames = Channel<StompFrame.Message>()
    private val sentFrames = Channel<StompFrame>()

    suspend fun waitForSentFrameAndSimulateCompletion(): StompFrame = sentFrames.receive()

    suspend fun simulateSubscriptionFrame(body: FrameBody?) {
        val headers = StompMessageHeaders("/dest", "42", "1234")
        val frame = StompFrame.Message(headers, body)
        incomingFrames.send(frame)
    }

    override suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt? {
        sentFrames.send(StompFrame.Send(headers, body))
        return null
    }

    override suspend fun <T> subscribe(
        headers: StompSubscribeHeaders,
        convertMessage: (StompFrame.Message) -> T
    ): StompSubscription<T> {
        val msgs: Channel<T> = Channel()
        val job = GlobalScope.launch {
            for (f in incomingFrames) {
                try {
                    val element = convertMessage(f)
                    msgs.send(element)
                } catch (e: Exception) {
                    msgs.close(e)
                }
            }
        }
        return object : StompSubscription<T> {
            override val id: String = "42"
            override val messages: ReceiveChannel<T> = msgs

            override suspend fun unsubscribe() {
                msgs.close()
                job.cancelAndJoin()
            }
        }
    }

    override suspend fun ack(ackId: String, transactionId: String?) {
        TODO("Not yet implemented")
    }

    override suspend fun nack(ackId: String, transactionId: String?) {
        TODO("Not yet implemented")
    }

    override suspend fun begin(transactionId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun commit(transactionId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun abort(transactionId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun disconnect() {
        TODO("Not yet implemented")
    }
}
