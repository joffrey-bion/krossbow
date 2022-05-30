package org.hildan.krossbow.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders

open class NoopStompSession : StompSession {

    override suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt? = null

    override suspend fun subscribe(headers: StompSubscribeHeaders): Flow<StompFrame.Message> = emptyFlow()

    override suspend fun ack(ackId: String, transactionId: String?) {
    }

    override suspend fun nack(ackId: String, transactionId: String?) {
    }

    override suspend fun begin(transactionId: String) {
    }

    override suspend fun commit(transactionId: String) {
    }

    override suspend fun abort(transactionId: String) {
    }

    override suspend fun disconnect() {
    }
}
