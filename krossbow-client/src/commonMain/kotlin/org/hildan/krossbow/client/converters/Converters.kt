package org.hildan.krossbow.client.converters

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.getContextualOrDefault
import org.hildan.krossbow.engines.KrossbowMessage
import org.hildan.krossbow.engines.MessageHeaders
import org.hildan.krossbow.engines.map
import kotlin.reflect.KClass

/**
 * Defines conversion of message payloads from byte arrays to Kotlin classes, and vice versa.
 */
interface MessageConverter {

    /**
     * Converts the given binary [message] into an instance of the given [clazz].
     */
    fun <T : Any> convertFromBytes(message: KrossbowMessage<ByteArray>, clazz: KClass<T>): KrossbowMessage<T>

    /**
     * Converts the given [value] of the given [clazz] into a [ByteArray].
     */
    fun <T : Any> convertToBytes(value: T, clazz: KClass<T>): ByteArray
}

/**
 * A [MessageConverter] extension that pre-converts binary messages to strings, and allows implementations to only
 * specify string message conversions.
 */
@UseExperimental(ExperimentalStdlibApi::class)
interface StringMessageConverter : MessageConverter {

    override fun <T : Any> convertFromBytes(message: KrossbowMessage<ByteArray>, clazz: KClass<T>): KrossbowMessage<T> {
        // TODO use encoding from headers (Content type/Mime type)
        return convertFromString(message.map { it.decodeToString() }, clazz)
    }

    /**
     * Converts the given text [message] into an instance of the given [clazz].
     */
    fun <T : Any> convertFromString(message: KrossbowMessage<String>, clazz: KClass<T>): KrossbowMessage<T>

    override fun <T : Any> convertToBytes(value: T, clazz: KClass<T>): ByteArray =
            convertToString(value, clazz).encodeToByteArray()

    /**
     * Converts the given [value] of the given [clazz] into a string.
     */
    fun <T : Any> convertToString(value: T, clazz: KClass<T>): String
}

/**
 * A [MessageConverter] extension that pre-converts binary messages to strings, and allows implementations to only
 * specify string message conversions. This is a simpler version of [StringMessageConverter] which does not allow to
 * modify the message headers, but provides a simpler API to implement.
 */
@UseExperimental(ExperimentalStdlibApi::class)
interface SimpleStringMessageConverter : MessageConverter {

    override fun <T : Any> convertFromBytes(message: KrossbowMessage<ByteArray>, clazz: KClass<T>): KrossbowMessage<T> {
        // TODO use encoding from headers (Content type/Mime type)
        return message.map { convertFromString(message.headers, it.decodeToString(), clazz) }
    }

    /**
     * Converts the given text [payload] into an instance of the given [clazz].
     */
    fun <T : Any> convertFromString(headers: MessageHeaders, payload: String, clazz: KClass<T>): T

    override fun <T : Any> convertToBytes(value: T, clazz: KClass<T>): ByteArray =
            convertToString(value, clazz).encodeToByteArray()

    /**
     * Converts the given [value] of the given [clazz] into a string.
     */
    fun <T : Any> convertToString(value: T, clazz: KClass<T>): String
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
    ) : SimpleStringMessageConverter {

        override fun <T : Any> convertFromString(headers: MessageHeaders, payload: String, clazz: KClass<T>): T {
            val serializer = json.context.getContextualOrDefault(clazz)
            return json.parse(serializer, payload)
        }

        override fun <T : Any> convertToString(value: T, clazz: KClass<T>): String {
            val serializer = json.context.getContextualOrDefault(clazz)
            return json.stringify(serializer, value)
        }
    }
}
