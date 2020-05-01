package org.hildan.krossbow.stomp.conversions.kxserialization

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerialModule
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.StompSubscription
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders

/**
 * Wraps this [StompSession] to add methods that can convert message bodies using the provided Kotlinx Serialization's
 * [BinaryFormat].
 *
 * The STOMP frames sent by the returned session have a binary body (of type [FrameBody.Binary]).
 * All frames with a non-null body are sent with a `content-type` header equal to [mediaType].
 */
fun StompSession.withBinaryConversions(format: BinaryFormat, mediaType: String): StompSessionWithKxSerialization =
    StompSessionWithBinaryConversions(this, format, mediaType)

/**
 * Wraps this [StompSession] to add methods that can convert message bodies using the provided Kotlinx Serialization's
 * [StringFormat].
 *
 * The STOMP frames sent by the returned session have a textual body (of type [FrameBody.Text]).
 * All frames with a non-null body are sent with a `content-type` header equal to [mediaType].
 */
fun StompSession.withTextConversions(format: StringFormat, mediaType: String): StompSessionWithKxSerialization =
    StompSessionWithTextConversions(this, format, mediaType)

/**
 * Wraps this [StompSession] to add methods that can convert message bodies using the provided Kotlinx Serialization's
 * [json].
 *
 * All frames with a non-null body are sent with a `content-type` header equal to [mediaType] (defaulting to
 * "application/json;charset=utf-8").
 */
fun StompSession.withJsonConversions(
    json: Json = Json(JsonConfiguration.Stable),
    mediaType: String = "application/json;charset=utf-8"
): StompSessionWithKxSerialization = withTextConversions(json, mediaType)

private abstract class BaseStompSessionWithConversions(
    session: StompSession,
    override val context: SerialModule,
    protected val mediaType: String
) : StompSession by session, StompSessionWithKxSerialization {

    override suspend fun <T : Any> convertAndSend(
        headers: StompSendHeaders,
        body: T?,
        serializer: SerializationStrategy<T>
    ): StompReceipt? {
        if (body == null) {
            return send(headers, null)
        }
        if (headers.contentType == null) {
            headers.contentType = mediaType
        }
        return send(headers, serializeBody(body, serializer))
    }

    protected abstract fun <T : Any> serializeBody(body: T?, serializer: SerializationStrategy<T>): FrameBody?

    override suspend fun <T : Any> subscribe(
        headers: StompSubscribeHeaders,
        deserializer: DeserializationStrategy<T>
    ): StompSubscription<T> = subscribe(headers) { frame ->
        deserializeOrNull(frame, deserializer)
            ?: error("Empty frame bodies are not allowed in this subscription, please use subscribeOptional() " +
                    "instead to allow them. Cannot deserialize object of type ${deserializer.descriptor.serialName} " +
                    "from null body")
    }

    override suspend fun <T : Any> subscribeOptional(
        headers: StompSubscribeHeaders,
        deserializer: DeserializationStrategy<T>
    ): StompSubscription<T?> = subscribe(headers) { frame -> deserializeOrNull(frame, deserializer) }

    protected abstract fun <T : Any> deserializeOrNull(
        frame: StompFrame.Message,
        deserializer: DeserializationStrategy<T>
    ): T?
}

private class StompSessionWithBinaryConversions(
    session: StompSession,
    val format: BinaryFormat,
    mediaType: String
) : BaseStompSessionWithConversions(session, format.context, mediaType) {

    override fun <T : Any> serializeBody(body: T?, serializer: SerializationStrategy<T>) =
        body?.let { FrameBody.Binary(format.dump(serializer, it)) }

    override fun <T : Any> deserializeOrNull(frame: StompFrame.Message, deserializer: DeserializationStrategy<T>) =
        frame.body?.bytes?.let { format.load(deserializer, it) }
}

private class StompSessionWithTextConversions(
    session: StompSession,
    val format: StringFormat,
    mediaType: String
) : BaseStompSessionWithConversions(session, format.context, mediaType) {

    override fun <T : Any> serializeBody(body: T?, serializer: SerializationStrategy<T>) =
        body?.let { FrameBody.Text(format.stringify(serializer, it)) }

    override fun <T : Any> deserializeOrNull(frame: StompFrame.Message, deserializer: DeserializationStrategy<T>) =
        frame.bodyAsText?.let { format.parse(deserializer, it) }
}
