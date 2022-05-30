package org.hildan.krossbow.stomp.conversions.jackson

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.conversions.KTypeRef
import org.hildan.krossbow.stomp.conversions.TypedStompSession
import org.hildan.krossbow.stomp.conversions.text.TextMessageConverter
import org.hildan.krossbow.stomp.conversions.text.withTextConversions
import java.lang.reflect.Type
import kotlin.reflect.javaType

/**
 * Wraps this [StompSession] to add methods that can convert message bodies using the provided Jackson [objectMapper].
 */
fun StompSession.withJackson(objectMapper: ObjectMapper = jacksonObjectMapper()): TypedStompSession =
    withTextConversions(JacksonConverter(objectMapper))

private class JacksonConverter(private val objectMapper: ObjectMapper) : TextMessageConverter {

    override val mediaType: String = "application/json"

    override fun <T> convertToString(value: T, type: KTypeRef<T>): String =
        objectMapper.writeValueAsString(value)

    override fun <T> convertFromString(text: String, type: KTypeRef<T>): T =
        objectMapper.readValue(text, type.toJacksonTypeReference())
}

@OptIn(ExperimentalStdlibApi::class)
private fun <T> KTypeRef<T>.toJacksonTypeReference(): TypeReference<T> = object : TypeReference<T>() {
    override fun getType(): Type = this@toJacksonTypeReference.kType.javaType
}
