package org.hildan.krossbow.stomp.conversions.text

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.charsets.*
import org.hildan.krossbow.stomp.conversions.KTypeRef
import org.hildan.krossbow.stomp.conversions.TypedStompSession
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders

/**
 * Wraps this [StompSession] to add methods that can convert message bodies using the provided [converter].
 */
fun StompSession.withTextConversions(converter: TextMessageConverter): TypedStompSession =
    SingleConverterStompSession(this, converter)

/**
 * Converts between text and objects.
 */
interface TextMessageConverter {

    /**
     * The media type produced by this converter (to use as content-type header).
     *
     * If the media type contains a `charset` parameter and it is different from UTF-8, frames are encoded to bytes
     * using the given charset, and sent as binary web socket frames (because text frames can only be UTF-8).
     * If no `charset` is specified, or if it is specified as UTF-8, then text web socket frames are sent, which means
     * they will be encoded as UTF-8.
     */
    val mediaType: String

    /**
     * Converts the given [value] object into a string based on the specified static [type].
     */
    fun <T> convertToString(value: T, type: KTypeRef<T>): String

    /**
     * Converts the given [text] string into an object of type [type].
     */
    fun <T> convertFromString(text: String, type: KTypeRef<T>): T
}

private class SingleConverterStompSession(
    private val session: StompSession,
    private val converter: TextMessageConverter,
) : StompSession by session, TypedStompSession {

    private val charset: Charset = extractCharset(converter.mediaType) ?: Charset.UTF_8
    private val sendBinaryFrames: Boolean = charset != Charset.UTF_8

    override suspend fun <T> convertAndSend(
        headers: StompSendHeaders,
        body: T,
        bodyType: KTypeRef<T>,
    ): StompReceipt? {
        if (headers.contentType == null) {
            headers.contentType = converter.mediaType
        }
        val bodyText = body?.let { converter.convertToString(it, bodyType) }
        val frameBody = bodyText?.let { createBody(it) }
        return send(headers, frameBody)
    }

    private fun createBody(bodyText: String): FrameBody = if (sendBinaryFrames) {
        FrameBody.Binary(bodyText.encodeToByteString(charset))
    } else {
        FrameBody.Text(bodyText)
    }

    override suspend fun <T> subscribe(headers: StompSubscribeHeaders, type: KTypeRef<T>): Flow<T> =
        subscribe(headers).map { frame ->
            converter.convertFromString(frame.bodyAsText, type)
        }
}
