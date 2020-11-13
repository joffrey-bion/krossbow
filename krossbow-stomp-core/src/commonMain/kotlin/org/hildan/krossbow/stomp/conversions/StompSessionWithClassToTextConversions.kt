package org.hildan.krossbow.stomp.conversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.io.charsets.Charset
import kotlinx.io.charsets.Charsets
import kotlinx.io.charsets.encodeToByteArray
import kotlinx.io.core.ExperimentalIoApi
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.utils.extractCharset
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
     * The MIME type produced by this converter (to use as content-type header).
     *
     * If the media type contains a `charset` parameter and it is different from UTF-8, frames are sent as binary web
     * socket frames (because text frames can only be UTF-8).
     * If no `charset` is specified, or if it is specified as UTF-8, then text web socket frames are sent, which means
     * they will be encoded as UTF-8.
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
    private val converter: TextMessageConverter,
) : StompSession by session, StompSessionWithClassConversions {

    private val charset: Charset = extractCharset(converter.mimeType) ?: Charsets.UTF_8
    private val sendBinaryFrames: Boolean = charset != Charsets.UTF_8

    override suspend fun <T : Any> convertAndSend(
        headers: StompSendHeaders,
        body: T?,
        bodyType: KClass<T>,
    ): StompReceipt? {
        if (headers.contentType == null) {
            headers.contentType = converter.mimeType
        }
        val bodyText = body?.let { converter.convertToString(it, bodyType) }
        val frameBody = bodyText?.let { createBody(it) }
        return send(headers, frameBody)
    }

    @OptIn(ExperimentalIoApi::class)
    private fun createBody(bodyText: String): FrameBody = if (sendBinaryFrames) {
        FrameBody.Binary(charset.newEncoder().encodeToByteArray(bodyText))
    } else {
        FrameBody.Text(bodyText)
    }

    override suspend fun <T : Any> subscribe(headers: StompSubscribeHeaders, clazz: KClass<T>): Flow<T> =
        subscribe(headers).map { frame ->
            convertOrNull(frame, clazz)
                ?: error("Empty bodies are not allowed in this subscription, please use subscribeOptional instead")
        }

    override suspend fun <T : Any> subscribeOptional(headers: StompSubscribeHeaders, clazz: KClass<T>): Flow<T?> =
        subscribe(headers).map { frame -> convertOrNull(frame, clazz) }

    private fun <T : Any> convertOrNull(msg: StompFrame.Message, clazz: KClass<T>): T? {
        val textBody = msg.bodyAsText
        if (textBody.isEmpty()) {
            return null
        }
        return converter.convertFromString(textBody, clazz)
    }
}
