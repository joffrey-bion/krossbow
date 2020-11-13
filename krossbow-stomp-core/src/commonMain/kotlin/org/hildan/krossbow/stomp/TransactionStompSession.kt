package org.hildan.krossbow.stomp

import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.headers.StompSendHeaders

/**
 * A special [StompSession] that automatically fills the `transaction` header (if absent) for all SEND, ACK, and NACK
 * frames.
 */
internal class TransactionStompSession(
    private val session: StompSession,
    private val transactionId: String,
) : StompSession by session {

    override suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt? {
        headers.transaction = headers.transaction ?: transactionId
        return session.send(headers, body)
    }

    override suspend fun ack(ackId: String, transactionId: String?) {
        session.ack(ackId, transactionId ?: this.transactionId)
    }

    override suspend fun nack(ackId: String, transactionId: String?) {
        session.nack(ackId, transactionId ?: this.transactionId)
    }
}
