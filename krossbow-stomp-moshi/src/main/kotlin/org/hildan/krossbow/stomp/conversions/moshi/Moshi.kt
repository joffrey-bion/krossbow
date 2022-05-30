package org.hildan.krossbow.stomp.conversions.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.conversions.KTypeRef
import org.hildan.krossbow.stomp.conversions.TypedStompSession
import org.hildan.krossbow.stomp.conversions.text.TextMessageConverter
import org.hildan.krossbow.stomp.conversions.text.withTextConversions

/**
 * Wraps this [StompSession] to add methods that can convert message bodies using the provided [Moshi] serializer.
 */
fun StompSession.withMoshi(moshi: Moshi = Moshi.Builder().build()): TypedStompSession =
    withTextConversions(MoshiConverter(moshi))

private class MoshiConverter(private val moshi: Moshi) : TextMessageConverter {

    override val mediaType: String = "application/json"

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T> convertToString(value: T, type: KTypeRef<T>): String {
        val adapter = moshi.adapter<T>(type.kType)
        return adapter.toJson(value)
    }

    // nullability is already verified by Moshi, so the cast from T? to T is safe
    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalStdlibApi::class)
    override fun <T> convertFromString(text: String, type: KTypeRef<T>): T {
        val adapter = moshi.adapter<T>(type.kType)
        return adapter.fromJson(text) as T
    }
}
