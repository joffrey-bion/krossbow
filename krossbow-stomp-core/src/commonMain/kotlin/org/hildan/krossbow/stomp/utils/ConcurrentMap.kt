package org.hildan.krossbow.stomp.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ConcurrentMap<K, V> {

    private val mutex = Mutex()

    private val map = mutableMapOf<K, V>()

    suspend fun values() = mutex.withLock { map.values }

    suspend fun get(key: K): V? = mutex.withLock { map[key] }

    suspend fun put(key: K, value: V) = mutex.withLock { map[key] = value }

    suspend fun remove(key: K) = mutex.withLock { map.remove(key) }
}
