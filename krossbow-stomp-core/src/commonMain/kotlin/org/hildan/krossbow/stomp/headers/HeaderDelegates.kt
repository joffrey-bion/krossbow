package org.hildan.krossbow.stomp.headers

import kotlin.reflect.KProperty

internal fun StompHeaders.header(customKey: String? = null) = header(customKey) { it }

internal fun StompHeaders.optionalHeader(customKey: String? = null, default: String? = null): HeaderDelegate<String?> =
    optionalHeader(customKey, default) { it }

internal inline fun <T> StompHeaders.optionalHeader(
    customKey: String? = null,
    crossinline transform: (String) -> T
): HeaderDelegate<T?> = optionalHeader(customKey, null, transform)

internal fun StompHeaders.mutableOptionalHeader(
    customKey: String? = null,
    default: String? = null
): MutableHeaderDelegate<String?> =
        mutableOptionalHeader(customKey, default, { it }, { it })

internal fun StompHeaders.mutableOptionalIntHeader(
    customKey: String? = null,
    default: Int? = null
): MutableHeaderDelegate<Int?> =
        mutableOptionalHeader(customKey, default, { it.toInt() }, { it.toString() })

internal inline fun <T> StompHeaders.header(customKey: String? = null, crossinline transform: (String) -> T) =
        HeaderDelegate(this, customKey) { value, key ->
            value?.let(transform) ?: throw IllegalStateException("missing required header '$key'")
        }

internal inline fun <T> StompHeaders.optionalHeader(
    customKey: String? = null,
    default: T,
    crossinline transform: (String) -> T
): HeaderDelegate<T> = HeaderDelegate(this, customKey) { value, _ -> value?.let(transform) ?: default }

internal inline fun <T> StompHeaders.mutableOptionalHeader(
    customKey: String? = null,
    default: T,
    crossinline getTransform: (String) -> T,
    noinline setTransform: (T) -> String?
): MutableHeaderDelegate<T> = MutableHeaderDelegate(
    rawHeaders = this,
    customName = customKey,
    getTransform = { value, _ -> value?.let(getTransform) ?: default },
    setTransform = setTransform
)

internal open class HeaderDelegate<T>(
    private val rawHeaders: StompHeaders,
    private val customName: String? = null,
    private val transform: (String?, String) -> T
) {
    operator fun getValue(thisRef: StompHeaders, property: KProperty<*>): T {
        val headerName = customName ?: property.name
        return transform(rawHeaders[headerName], headerName)
    }
}

internal class MutableHeaderDelegate<T>(
    private val rawHeaders: StompHeaders,
    private val customName: String? = null,
    getTransform: (String?, String) -> T,
    private val setTransform: (T) -> String?
) : HeaderDelegate<T>(rawHeaders, customName, getTransform) {

    operator fun setValue(thisRef: StompHeaders, property: KProperty<*>, value: T) {
        val headerName = customName ?: property.name
        val strValue = setTransform(value)
        if (strValue == null) {
            rawHeaders.remove(headerName)
        } else {
            rawHeaders[headerName] = strValue
        }
    }
}
