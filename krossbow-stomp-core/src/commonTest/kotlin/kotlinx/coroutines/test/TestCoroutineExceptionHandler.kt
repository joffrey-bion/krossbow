/*
 * Copyright 2016-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.test

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Access uncaught coroutine exceptions captured during test execution.
 */
public interface UncaughtExceptionCaptor {
    /**
     * List of uncaught coroutine exceptions.
     *
     * The returned list is a copy of the currently caught exceptions.
     * During [cleanupTestCoroutines] the first element of this list is rethrown if it is not empty.
     */
    public val uncaughtExceptions: List<Throwable>

    /**
     * Call after the test completes to ensure that there were no uncaught exceptions.
     *
     * The first exception in uncaughtExceptions is rethrown. All other exceptions are
     * printed using [Throwable.printStackTrace].
     *
     * @throws Throwable the first uncaught exception, if there are any uncaught exceptions.
     */
    public fun cleanupTestCoroutines()
}

/**
 * An exception handler that captures uncaught exceptions in tests.
 */
public class TestCoroutineExceptionHandler : AbstractCoroutineContextElement(CoroutineExceptionHandler),
    UncaughtExceptionCaptor, CoroutineExceptionHandler {
    private val mutex = Mutex()
    private val _exceptions = mutableListOf<Throwable>()

    /** @suppress **/
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        mutex.withActiveNonSuspendingLock {
            _exceptions += exception
        }
    }

    /** @suppress **/
    override val uncaughtExceptions: List<Throwable>
        get() = mutex.withActiveNonSuspendingLock { _exceptions.toList() }

    /** @suppress **/
    override fun cleanupTestCoroutines() {
        mutex.withActiveNonSuspendingLock {
            val exception = _exceptions.firstOrNull() ?: return
            // log the rest
            _exceptions.drop(1).forEach { it.printStackTrace() }
            throw exception
        }
    }
}

// horrible hack to replace synchronized(_exceptions) blocks
inline fun <T> Mutex.withActiveNonSuspendingLock(action: () -> T): T {
    while(!tryLock()) {
        // retry
    }
    return try {
        action()
    } finally {
        unlock()
    }
}
