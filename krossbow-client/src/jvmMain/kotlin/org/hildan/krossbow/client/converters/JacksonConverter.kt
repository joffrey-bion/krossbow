package org.hildan.krossbow.client.converters

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hildan.krossbow.engines.MessageHeaders
import kotlin.reflect.KClass

class JacksonConverter(
    val objectMapper: ObjectMapper = jacksonObjectMapper()
) : SimpleStringMessageConverter {

    override fun <T : Any> convertFromString(headers: MessageHeaders, payload: String, clazz: KClass<T>): T {
        return objectMapper.readValue(payload, clazz.java)
    }

    override fun <T : Any> convertToString(payload: T, clazz: KClass<T>): String {
        return objectMapper.writeValueAsString(payload)
    }
}
