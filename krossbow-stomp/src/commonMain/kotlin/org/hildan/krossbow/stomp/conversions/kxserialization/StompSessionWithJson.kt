package org.hildan.krossbow.stomp.conversions.kxserialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerialModule
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.StompSubscription
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.asText
import org.hildan.krossbow.stomp.headers.StompSendHeaders

/**
 * Wraps this [StompSession] to add methods that can convert message bodies using the provided Kotlinx Serialization's
 * [json].
 */
fun StompSession.withJsonConversions(json: Json = Json(JsonConfiguration.Stable)): StompSessionWithKxSerialization =
        StompSessionWithJson(this, json)

internal class StompSessionWithJson(
    session: StompSession,
    private val json: Json
) : StompSession by session, StompSessionWithKxSerialization {

    override val context: SerialModule = json.context

    override suspend fun <T : Any> convertAndSend(
        headers: StompSendHeaders,
        body: T?,
        serializer: SerializationStrategy<T>
    ): StompReceipt? {
        if (body == null) {
            return send(headers, null)
        }
        if (headers.contentType == null) {
            headers.contentType = "application/json"
        }
        val textBody = json.stringify(serializer, body)
        return send(headers, FrameBody.Text(textBody))
    }

    override suspend fun <T : Any> subscribe(
        destination: String,
        deserializer: DeserializationStrategy<T>,
        receiptId: String?
    ): StompSubscription<T> = subscribe(destination, receiptId) { msg ->
        requireNotNull(msg.body) {
            "Cannot deserialize object of type ${deserializer.descriptor.name} from null body"
        }
        msg.body.deserialize(deserializer)
    }

    override suspend fun <T : Any> subscribeOptional(
        destination: String,
        deserializer: DeserializationStrategy<T>,
        receiptId: String?
    ): StompSubscription<T?> = subscribe(destination, receiptId) { msg ->
        if (msg.body == null) {
            null
        } else {
            msg.body.deserialize(deserializer)
        }
    }

    private fun <T : Any> FrameBody.deserialize(deserializer: DeserializationStrategy<T>) =
            json.parse(deserializer, asText())
}
