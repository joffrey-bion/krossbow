package org.hildan.krossbow.stomp.headers

import kotlin.properties.*
import kotlin.reflect.*

/**
 * An optional string header with the given [name].
 * The header doesn't have to be present in the frame.
 * If the header is absent, the property is null.
 */
internal fun optionalHeader(name: String): HeaderDelegate<String?> =
    optionalHeader(name, default = null, { it }, { it })

/**
 * A required string header with the given [name].
 * The header has to be present in the frame.
 * The property throws an exception if it is accessed before a value was set.
 */
internal fun requiredHeader(name: String): HeaderDelegate<String> = HeaderDelegate(
    headerName = name,
    getTransform = { value -> value ?: error("missing required header '$name'") },
    setTransform = { it },
)

/**
 * A required string header with the given [name].
 *
 * The header has to be present in the frame, but we provide a sensible default for the user.
 * The header is immediately set to the given [preset] value if it is absent from the initial map, so it is always
 * encoded even without user action.
 */
internal fun MapBasedStompHeaders.requiredHeader(name: String, preset: String): HeaderDelegate<String> {
    if (!backingMap.containsKey(name)) {
        this[name] = preset
    }
    return requiredHeader(name)
}

/**
 * A required header with the given [name].
 *
 * The header has to be present in the frame, but we provide a sensible default for the user.
 * The header is immediately set to the given [preset] value if it is absent from the initial map, so it is always
 * encoded even without user action.
 */
internal fun <T> MapBasedStompHeaders.requiredHeader(
    name: String,
    preset: T,
    decode: (String) -> T,
    encode: (T) -> String?,
): HeaderDelegate<T> {
    if (!backingMap.containsKey(name)) {
        this[name] = encode(preset)
    }
    return HeaderDelegate(
        headerName = name,
        getTransform = { value -> value?.let(decode) ?: error("missing required header '$name'") },
        setTransform = encode,
    )
}

/**
 * An optional header with the given [name].
 * If the header is absent, the property returns the given [default] value.
 *
 * The value of the property is converted from and to strings using the [decode] and [encode] functions respectively.
 */
internal fun <T> optionalHeader(
    name: String,
    default: T,
    decode: (String) -> T,
    encode: (T) -> String?,
): HeaderDelegate<T> = HeaderDelegate(
    headerName = name,
    getTransform = { value -> value?.let(decode) ?: default },
    setTransform = encode,
)

internal class HeaderDelegate<T>(
    private val headerName: String,
    private val getTransform: (String?) -> T,
    private val setTransform: (T) -> String?,
) : ReadWriteProperty<MapBasedStompHeaders, T> {

    override operator fun getValue(thisRef: MapBasedStompHeaders, property: KProperty<*>): T {
        return getTransform(thisRef.backingMap[headerName])
    }

    override operator fun setValue(thisRef: MapBasedStompHeaders, property: KProperty<*>, value: T) {
        val strValue = setTransform(value)
        if (strValue == null) {
            thisRef.backingMap.remove(headerName)
        } else {
            thisRef.backingMap[headerName] = strValue
        }
    }
}
