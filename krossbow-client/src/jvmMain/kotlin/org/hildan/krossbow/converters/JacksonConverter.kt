package org.hildan.krossbow.converters

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hildan.krossbow.stomp.StompMessage
import org.hildan.krossbow.stomp.headers.StompMessageHeaders
import kotlin.reflect.KClass

/**
 * A [MessageConverter] implementation using Jackson for JSON conversion.
 */
class JacksonConverter(
    /**
     * The [ObjectMapper] to use for conversions. Defaults to [jacksonObjectMapper] from the Kotlin module of Jackson.
     */
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) : TextMessageConverter {

    override fun <T : Any> convertFromString(
        headers: StompMessageHeaders,
        payload: String?,
        clazz: KClass<T>
    ): StompMessage<T> {
        return StompMessage(objectMapper.readValue(payload, clazz.java), headers)
    }

    override fun <T : Any> convertToString(value: T?, clazz: KClass<T>): String? {
        return objectMapper.writeValueAsString(value)
    }
}
