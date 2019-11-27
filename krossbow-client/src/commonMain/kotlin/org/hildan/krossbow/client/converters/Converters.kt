package org.hildan.krossbow.client.converters

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.getContextualOrDefault
import org.hildan.krossbow.client.MessageConverter
import org.hildan.krossbow.engines.KrossbowMessage
import org.hildan.krossbow.engines.MessageHeaders
import org.hildan.krossbow.engines.map
import kotlin.reflect.KClass

@UseExperimental(ExperimentalStdlibApi::class)
interface StringMessageConverter : MessageConverter {

    override fun <T : Any> convertFromBytes(message: KrossbowMessage<ByteArray>, clazz: KClass<T>): KrossbowMessage<T> {
        // TODO use encoding from headers (Content type/Mime type)
        return convertFromString(message.map { it.decodeToString() }, clazz)
    }

    fun <T : Any> convertFromString(message: KrossbowMessage<String>, clazz: KClass<T>): KrossbowMessage<T>

    override fun <T : Any> convertToBytes(value: T, clazz: KClass<T>): ByteArray =
            convertToString(value, clazz).encodeToByteArray()

    fun <T : Any> convertToString(payload: T, clazz: KClass<T>): String
}

@UseExperimental(ExperimentalStdlibApi::class)
interface SimpleStringMessageConverter : MessageConverter {

    override fun <T : Any> convertFromBytes(message: KrossbowMessage<ByteArray>, clazz: KClass<T>): KrossbowMessage<T> {
        // TODO use encoding from headers (Content type/Mime type)
        return message.map { convertFromString(message.headers, it.decodeToString(), clazz) }
    }

    fun <T : Any> convertFromString(headers: MessageHeaders, payload: String, clazz: KClass<T>): T

    override fun <T : Any> convertToBytes(value: T, clazz: KClass<T>): ByteArray =
            convertToString(value, clazz).encodeToByteArray()

    fun <T : Any> convertToString(payload: T, clazz: KClass<T>): String
}

@UseExperimental(ImplicitReflectionSerializer::class, ExperimentalStdlibApi::class)
sealed class KotlinxSerialization {

    class JsonConverter(
        private val json: Json = Json(JsonConfiguration.Stable)
    ) : SimpleStringMessageConverter {

        override fun <T : Any> convertFromString(headers: MessageHeaders, payload: String, clazz: KClass<T>): T {
            val serializer = json.context.getContextualOrDefault(clazz)
            return json.parse(serializer, payload)
        }

        override fun <T : Any> convertToString(payload: T, clazz: KClass<T>): String {
            val serializer = json.context.getContextualOrDefault(clazz)
            return json.stringify(serializer, payload)
        }
    }
}
