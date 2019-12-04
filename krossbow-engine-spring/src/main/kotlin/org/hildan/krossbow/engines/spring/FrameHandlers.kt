package org.hildan.krossbow.engines.spring

import org.hildan.krossbow.engines.InvalidFramePayloadException
import org.hildan.krossbow.engines.KrossbowMessage
import org.hildan.krossbow.engines.MessageHeaders
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import java.lang.reflect.Type

/**
 * An implementation of [StompFrameHandler] that expects messages with binary payloads.
 */
internal class BinaryFrameHandler(
    private val onReceive: (KrossbowMessage<ByteArray>) -> Unit
) : StompFrameHandler {

    override fun getPayloadType(headers: StompHeaders): Type = ByteArray::class.java

    override fun handleFrame(headers: StompHeaders, payload: Any?) {
        onReceive(KrossbowMessage(payload as ByteArray, headers.toKrossbowHeaders()))
    }
}

/**
 * An implementation of [StompFrameHandler] that expects messages without payloads only.
 */
internal class NoPayloadFrameHandler(
    private val onReceive: (KrossbowMessage<Unit>) -> Unit
) : StompFrameHandler {

    override fun getPayloadType(stompHeaders: StompHeaders): Type = Unit.javaClass

    override fun handleFrame(stompHeaders: StompHeaders, payload: Any?) {
        if (payload != null) {
            throw InvalidFramePayloadException("No payload was expected but some content was received")
        }
        onReceive(KrossbowMessage(Unit, stompHeaders.toKrossbowHeaders()))
    }
}

private fun StompHeaders.toKrossbowHeaders(): MessageHeaders = object : MessageHeaders {} // TODO fill them up
