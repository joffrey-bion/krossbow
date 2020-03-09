package org.hildan.krossbow.stomp.conversions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.StompSubscription
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.asText
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import kotlin.reflect.KClass

/**
 * Wraps this [StompSession] to add methods that can convert message bodies using the provided Jackson [objectMapper].
 */
fun StompSession.withJacksonConversions(objectMapper: ObjectMapper = jacksonObjectMapper()): StompSessionWithReflection =
        StompSessionWithJackson(this, objectMapper)

internal class StompSessionWithJackson(
    session: StompSession,
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) : StompSession by session, StompSessionWithReflection {

    override suspend fun <T : Any> convertAndSend(
        headers: StompSendHeaders,
        body: T?,
        bodyType: KClass<T>
    ): StompReceipt? {
        if (headers.contentType == null) {
            headers.contentType = "application/json"
        }
        return send(headers, FrameBody.Text(objectMapper.writeValueAsString(body)))
    }

    override suspend fun <T : Any> subscribe(
        destination: String,
        clazz: KClass<T>,
        receiptId: String?
    ): StompSubscription<T> = subscribe(destination, receiptId) { msg ->
        requireNotNull(msg.body) {
            "Empty messages are not allowed in this subscription, please use subscribeOptional instead"
        }
        objectMapper.readValue(msg.body.asText(), clazz.java)
    }

    override suspend fun <T : Any> subscribeOptional(
        destination: String,
        clazz: KClass<T>,
        receiptId: String?
    ): StompSubscription<T?> = subscribe(destination, receiptId) { msg ->
        if (msg.body == null) {
            null
        } else {
            objectMapper.readValue(msg.body.asText(), clazz.java)
        }
    }
}
