package org.hildan.krossbow.stomp.conversions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hildan.krossbow.stomp.StompSession
import kotlin.reflect.KClass

/**
 * Wraps this [StompSession] to add methods that can convert message bodies using the provided Jackson [objectMapper].
 */
@Deprecated(
    message = "This yields a session that gets type information via KClass<T>, " +
        "it doesn't handle generic types properly",
    ReplaceWith(
        expression = "this.withJackson(objectMapper)",
        imports = ["org.hildan.krossbow.stomp.conversions.jackson.withJackson"],
    )
)
fun StompSession.withJacksonConversions(
    objectMapper: ObjectMapper = jacksonObjectMapper(),
): StompSessionWithClassConversions = withTextConversions(JacksonConverter(objectMapper))

private class JacksonConverter(private val objectMapper: ObjectMapper) : TextMessageConverter {

    override val mimeType: String = "application/json"

    override fun <T : Any> convertToString(body: T, bodyType: KClass<T>): String = objectMapper.writeValueAsString(body)

    override fun <T : Any> convertFromString(body: String, bodyType: KClass<T>): T =
        objectMapper.readValue(body, bodyType.java)
}
