package org.hildan.krossbow.stomp.headers

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Delegated properties for StompHeaders
 *
 * The functions here assist in creating a 'HeaderDelegate' or a 'MutableHeaderDelegate'. These delegates are useful
 * in defining properties for 'StompHeaders' objects ('MutableMap<String, String>' values). Essentially, they
 * encapsulate these properties with an option of 'customName', and also apply transformation operations during
 * 'getValue' and 'setValue' calls.
 *
 * These components enable the creation of varied delegates that can apply constraints (either mandatory or optional)
 * and transformations (like converting a header string value to a different type or providing a default value)
 * on these header values.
 */

/**
 * Mandatory header for type String without transformations
 */
internal fun header(customKey: String? = null): HeaderDelegate<String> = header(customKey) { it }

/**
 * Optional header for type String without transformations
 */
internal fun optionalHeader(customKey: String? = null): HeaderDelegate<String?> = optionalHeader(customKey) { it }

/**
 * Optional header with transformed getValue to T
 */
internal inline fun <T> optionalHeader(
    customKey: String? = null,
    crossinline transform: (String) -> T,
): HeaderDelegate<T?> = optionalHeader(customKey, null, transform)

/**
 * Mutable Optional header with default value of type String without transformation
 */
internal fun mutableOptionalHeader(customKey: String? = null, default: String? = null): MutableHeaderDelegate<String?> =
    mutableOptionalHeader(customKey, default, { it }, { it })

/**
 * Mutable Optional header with default value of type Int
 */
internal fun mutableOptionalIntHeader(customKey: String? = null, default: Int? = null): MutableHeaderDelegate<Int?> =
    mutableOptionalHeader(customKey, default, { it.toInt() }, { it.toString() })


/**
 * Mandatory header with generic transform
 */
internal inline fun <T> header(customKey: String? = null, crossinline transform: (String) -> T): HeaderDelegate<T> =
    HeaderDelegate(customKey) { value, key ->
        value?.let(transform) ?: throw IllegalStateException("missing required header '$key'")
    }

internal inline fun <T> optionalHeader(
    customKey: String? = null,
    default: T,
    crossinline transform: (String) -> T,
): HeaderDelegate<T> = HeaderDelegate(customKey) { value, _ -> value?.let(transform) ?: default }

/**
 * Generic mutable optional header with a default value (can be null).
 */
internal inline fun <T> mutableOptionalHeader(
    customKey: String? = null,
    default: T,
    crossinline getTransform: (String) -> T,
    noinline setTransform: (T) -> String?,
): MutableHeaderDelegate<T> = MutableHeaderDelegate(
    customName = customKey,
    getTransform = { value, _ -> value?.let(getTransform) ?: default },
    setTransform = setTransform,
)

internal open class HeaderDelegate<T>(
    private val customName: String? = null,
    private val transform: (String?, String) -> T,
) : ReadOnlyProperty<StompHeaders, T> {

    override operator fun getValue(thisRef: StompHeaders, property: KProperty<*>): T {
        val headerName = customName ?: property.name
        return transform(thisRef[headerName], headerName)
    }
}

internal class MutableHeaderDelegate<T>(
    private val customName: String? = null,
    getTransform: (String?, String) -> T,
    private val setTransform: (T) -> String?,
) : HeaderDelegate<T>(customName, getTransform), ReadWriteProperty<StompHeaders, T> {

    override operator fun setValue(thisRef: StompHeaders, property: KProperty<*>, value: T) {
        val headerName = customName ?: property.name
        val strValue = setTransform(value)
        if (strValue == null) {
            thisRef.remove(headerName)
        } else {
            thisRef[headerName] = strValue
        }
    }
}
