package org.hildan.krossbow.stomp.conversions

import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Creates a [KTypeRef] representing the given type [T].
 */
inline fun <reified T> typeRefOf(): KTypeRef<T> = KTypeRef(typeOf<T>())

/**
 * Wraps a [KType] instance with a generic type parameter, to ensure type-safe usages.
 *
 * Using [KType] directly, we can't guarantee that the method type parameter matches the instance of `KType` that we
 * get:
 *
 * ```kotlin
 * fun <T> doSomething(returnType: KType): T {
 *     // ...
 * }
 *
 * // could be misused like this
 * doSomething<String>(typeOf<Int>()) // mismatch String vs Int
 * ```
 */
class KTypeRef<T> @PublishedApi internal constructor(val kType: KType)
