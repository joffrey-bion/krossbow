package org.hildan.krossbow.client.headers

import org.hildan.krossbow.client.frame.toHeartBeat
import kotlin.reflect.KProperty

internal fun StompHeaders.acceptVersionHeader() = header(HeaderKeys.ACCEPT_VERSION) { it.split(',') }

internal fun StompHeaders.heartBeatHeader() = optionalHeader(HeaderKeys.HEART_BEAT) { it.toHeartBeat() }

internal fun StompHeaders.contentLengthHeader() = optionalHeader(HeaderKeys.CONTENT_LENGTH) { it.toLong() }

internal fun StompHeaders.header(customKey: String? = null) = header(customKey) { it }

internal inline fun <T> StompHeaders.header(customKey: String? = null, crossinline transform: (String) -> T) =
    HeaderDelegate(this, customKey) { value, key ->
        value?.let { transform(it) } ?: throw IllegalStateException("missing required header '$key'")
    }

internal fun StompHeaders.optionalHeader(customKey: String? = null, default: String? = null): HeaderDelegate<String?> =
    optionalHeader(customKey, default) { it }

internal inline fun <T> StompHeaders.optionalHeader(
    customKey: String? = null,
    crossinline transform: (String) -> T
): HeaderDelegate<T?> =
    optionalHeader(customKey, null, transform)

internal inline fun <T> StompHeaders.optionalHeader(
    customKey: String? = null,
    default: T,
    crossinline transform: (String) -> T
): HeaderDelegate<T> =
    HeaderDelegate(this, customKey) { value, _ ->
        value?.let { transform(it) } ?: default
    }

internal class HeaderDelegate<T>(
    private val rawHeaders: StompHeaders,
    private val customName: String? = null,
    private val transform: (String?, String) -> T
) {
    operator fun getValue(thisRef: StompHeaders, property: KProperty<*>): T {
        val headerName = customName ?: property.name
        return transform(rawHeaders.getValue(headerName), headerName)
    }
}
