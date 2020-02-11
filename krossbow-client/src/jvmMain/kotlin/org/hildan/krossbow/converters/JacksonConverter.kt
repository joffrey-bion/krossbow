package org.hildan.krossbow.converters

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hildan.krossbow.stomp.headers.StompHeaders
import kotlin.reflect.KClass

/**
 * A [MessageConverter] implementation using Jackson for JSON conversion.
 */
class JacksonConverter(
    /**
     * The [ObjectMapper] to use for conversions. Defaults to [jacksonObjectMapper] from the Kotlin module of Jackson.
     */
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) : SimpleStringMessageConverter {

    override fun <T : Any> convertFromString(headers: StompHeaders, payload: String, clazz: KClass<T>): T {
        return objectMapper.readValue(payload, clazz.java)
    }

    override fun <T : Any> convertToString(value: T, clazz: KClass<T>): String {
        return objectMapper.writeValueAsString(value)
    }
}
