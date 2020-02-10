package org.hildan.krossbow.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SuspendingAtomicInt(private var value: Int = 0) {

    private val mutex = Mutex()

    // TODO maybe change this to actor to completely avoid locking
    // https://kotlinlang.org/docs/reference/coroutines/shared-mutable-state-and-concurrency.html#actors
    suspend fun getAndIncrement(): Int = mutex.withLock { value++ }
}
