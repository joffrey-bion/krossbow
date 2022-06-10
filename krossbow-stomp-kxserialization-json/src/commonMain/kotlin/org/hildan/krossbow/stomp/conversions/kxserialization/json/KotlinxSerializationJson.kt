package org.hildan.krossbow.stomp.conversions.kxserialization.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.conversions.kxserialization.StompSessionWithKxSerialization
import org.hildan.krossbow.stomp.conversions.kxserialization.withTextConversions

/**
 * Wraps this [StompSession] to add methods that can convert message bodies using the configuration from the provided
 * Kotlinx Serialization's [json], optionally adjusted with [configure].
 *
 * All frames with a non-null body are sent with a `content-type` header equal to [mediaType] (defaulting to
 * "application/json;charset=utf-8").
 */
fun StompSession.withJsonConversions(
    json: Json = Json,
    mediaType: String = "application/json;charset=utf-8",
    configure: JsonBuilder.() -> Unit = {},
): StompSessionWithKxSerialization = withTextConversions(Json(json, configure), mediaType)
