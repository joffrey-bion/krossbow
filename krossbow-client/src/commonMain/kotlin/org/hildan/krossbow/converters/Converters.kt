package org.hildan.krossbow.converters

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.getContextualOrDefault
import org.hildan.krossbow.stomp.StompMessage
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.frame.asText
import org.hildan.krossbow.stomp.headers.StompMessageHeaders
import kotlin.reflect.KClass

/**
 * Defines conversions between frames and typed messages.
 */
interface MessageConverter {

    /**
     * Converts the given MESSAGE [frame] into a [StompMessage] with a payload of the given [clazz].
     */
    fun <T : Any> deserialize(frame: StompFrame.Message, clazz: KClass<T>): StompMessage<T>

    /**
     * Converts the given [value] of the given [clazz] into a [FrameBody].
     */
    fun <T : Any> serialize(value: T?, clazz: KClass<T>): FrameBody?
}

/**
 * A [MessageConverter] that pre-converts binary messages to strings, and allows implementations to only specify
 * text conversions.
 */
interface TextMessageConverter : MessageConverter {

    override fun <T : Any> deserialize(frame: StompFrame.Message, clazz: KClass<T>): StompMessage<T> {
        // TODO use encoding from headers (Content type/Mime type)
        // but double check if text isn't forced to be UTF-8 because of underlying websocket specification
        val payloadText = frame.body.asText()
        return convertFromString(frame.headers, payloadText, clazz)
    }

    /**
     * Converts the given text [payload] into a [StompMessage] with a payload of the given [clazz].
     */
    fun <T : Any> convertFromString(headers: StompMessageHeaders, payload: String?, clazz: KClass<T>): StompMessage<T>

    override fun <T : Any> serialize(value: T?, clazz: KClass<T>): FrameBody? {
        val text = convertToString(value, clazz)
        return text?.let { FrameBody.Text(it) }
    }

    /**
     * Converts the given [value] of the given [clazz] into a string.
     */
    fun <T : Any> convertToString(value: T?, clazz: KClass<T>): String?
}

/**
 * A container class for [MessageConverter] implementations using Kotlinx Serialization.
 */
@UseExperimental(ImplicitReflectionSerializer::class, ExperimentalStdlibApi::class)
class KotlinxSerialization {

    /**
     * A [MessageConverter] implementation using Kotlinx Serialization's JSON support.
     */
    class JsonConverter(
        private val json: Json = Json(JsonConfiguration.Stable)
    ) : TextMessageConverter {

        override fun <T : Any> convertFromString(
            headers: StompMessageHeaders,
            payload: String?,
            clazz: KClass<T>
        ): StompMessage<T> {
            if (payload == null) {
                throw RuntimeException("Cannot create object of type $clazz from a MESSAGE frame without body")
            }
            val serializer = json.context.getContextualOrDefault(clazz)
            return StompMessage(json.parse(serializer, payload), headers)
        }

        override fun <T : Any> convertToString(value: T?, clazz: KClass<T>): String? {
            if (value == null) {
                return null
            }
            val serializer = json.context.getContextualOrDefault(clazz)
            return json.stringify(serializer, value)
        }
    }
}
