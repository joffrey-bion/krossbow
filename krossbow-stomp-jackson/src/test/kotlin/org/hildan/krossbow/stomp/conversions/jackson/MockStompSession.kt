package org.hildan.krossbow.stomp.conversions.jackson

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import org.hildan.krossbow.stomp.*
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

    @OptIn(UnsafeStompSessionApi::class)
    override suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt? =
        sendRawFrameAndMaybeAwaitReceipt(StompFrame.Send(headers, body))

    override suspend fun subscribe(headers: StompSubscribeHeaders): Flow<StompFrame.Message> =
        incomingFrames.consumeAsFlow()

    @UnsafeStompSessionApi
    override suspend fun sendRawFrameAndMaybeAwaitReceipt(frame: StompFrame): StompReceipt? {
        sentFrames.send(frame)
        return frame.headers.receipt?.let { StompReceipt(it) }
    }

    override suspend fun disconnect() = error("This mock doesn't support this method")
}
