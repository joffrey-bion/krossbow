package org.hildan.krossbow.stomp.conversions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.charsets.*
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import kotlin.reflect.KClass

/**
 * Wraps this [StompSession] to add methods that can convert message bodies using the provided [converter].
 */
@Suppress("DeprecatedCallableAddReplaceWith") // no direct replacement possible
@Deprecated(
    message = "This yields a session that gets type information via KClass<T>, " +
        "it doesn't handle generic types properly and will be removed in a future version. " +
        "Please use withTextConversions() and TextMessageConverter from the package " +
        "org.hildan.krossbow.stomp.conversions.text, which is based on proper type references.",
)
fun StompSession.withTextConversions(converter: TextMessageConverter): StompSessionWithClassConversions =
    StompSessionWithClassToTextConversions(this, converter)

/**
 * Converts between text and objects based on a [KClass].
 */
@Deprecated(
    message = "This converter interface passes type information via KClass<T>, " +
        "it doesn't handle generic types like List<CustomClass> properly. " +
        "Use a dedicated alternative for your converter.",
)
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

    private fun createBody(bodyText: String): FrameBody = if (sendBinaryFrames) {
        FrameBody.Binary(bodyText.encodeToBytes(charset))
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
