package org.hildan.krossbow.stomp.conversions

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
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

    override suspend fun subscribe(headers: StompSubscribeHeaders): Flow<StompFrame.Message> =
        incomingFrames.consumeAsFlow()

    override suspend fun ack(ackId: String, transactionId: String?) = error("This mock doesn't support this method")

    override suspend fun nack(ackId: String, transactionId: String?) = error("This mock doesn't support this method")

    override suspend fun begin(transactionId: String) = error("This mock doesn't support this method")

    override suspend fun commit(transactionId: String) = error("This mock doesn't support this method")

    override suspend fun abort(transactionId: String) = error("This mock doesn't support this method")

    override suspend fun disconnect() = error("This mock doesn't support this method")
}
