package org.hildan.krossbow.stomp.conversions

import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.StompSubscription
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.asText
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import kotlin.reflect.KClass

/**
 * Wraps this [StompSession] to add methods that can convert message bodies using the provided [converter].
 */
fun StompSession.withTextConversions(converter: TextMessageConverter): StompSessionWithClassConversions =
        StompSessionWithClassToTextConversions(this, converter)

/**
 * Converts between text and objects based on a [KClass].
 */
interface TextMessageConverter {

    /**
     * The MIME type produced by this converter (to use as content-type header)
     */
    val mimeType: String

    /**
     * Converts the given [body] object into a string.
     */
    fun <T : Any> convertToString(body: T, bodyType: KClass<T>): String

    /**
     * Converts the given [body] string into an object of type [bodyType].
     */
    fun <T : Any> convertFromString(body: String, bodyType: KClass<T>): T
}

internal class StompSessionWithClassToTextConversions(
    private val session: StompSession,
    private val converter: TextMessageConverter
) : StompSession by session, StompSessionWithClassConversions {

    override suspend fun <T : Any> convertAndSend(
        headers: StompSendHeaders,
        body: T?,
        bodyType: KClass<T>
    ): StompReceipt? {
        if (headers.contentType == null) {
            headers.contentType = converter.mimeType
        }
        val frameBody = body?.let { FrameBody.Text(converter.convertToString(it, bodyType)) }
        return send(headers, frameBody)
    }

    override suspend fun <T : Any> subscribe(headers: StompSubscribeHeaders, clazz: KClass<T>): StompSubscription<T> =
        subscribe(headers) { msg ->
            requireNotNull(msg.body) {
                "Empty messages are not allowed in this subscription, please use subscribeOptional instead"
            }
            converter.convertFromString(msg.body.asText(), clazz)
        }

    override suspend fun <T : Any> subscribeOptional(
        headers: StompSubscribeHeaders,
        clazz: KClass<T>
    ): StompSubscription<T?> = subscribe(headers) { msg ->
        if (msg.body == null) {
            null
        } else {
            converter.convertFromString(msg.body.asText(), clazz)
        }
    }
}
