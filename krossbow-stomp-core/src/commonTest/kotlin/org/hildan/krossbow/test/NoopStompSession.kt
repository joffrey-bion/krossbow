package org.hildan.krossbow.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.hildan.krossbow.stomp.*
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders

open class NoopStompSession : StompSession {

    override suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt? = null

    override suspend fun subscribe(headers: StompSubscribeHeaders): Flow<StompFrame.Message> = emptyFlow()

    @UnsafeStompSessionApi
    override suspend fun sendRawFrameAndMaybeAwaitReceipt(frame: StompFrame): StompReceipt? = null

    override suspend fun disconnect() = Unit
}
