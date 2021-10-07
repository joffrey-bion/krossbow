package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.*
import platform.Foundation.*
import kotlin.system.getTimeNanos
import kotlin.test.Ignore
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

actual typealias IgnoreOnNative = Ignore

@OptIn(ExperimentalTime::class, DelicateCoroutinesApi::class)
actual fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit) = runOnMainThreadAlongMainLoop { block() }

/**
 * Runs the given test [block] on the main thread, while still executing the main loop concurrently to make sure every
 * queued operation (in [NSOperationQueue.mainQueue]) can progress too.
 *
 * Inspired by https://github.com/ktorio/ktor/issues/678#issuecomment-433756753
 */
@OptIn(ExperimentalTime::class, DelicateCoroutinesApi::class)
private fun runOnMainThreadAlongMainLoop(timeout: Duration = Duration.seconds(10), block: suspend CoroutineScope.() ->
Unit) {
    val maxTimeNanos = getTimeNanos() + timeout.inWholeNanoseconds

    // The block is run in a concurrent coroutine, so we can run the main loop at the same time.
    // It's run on the main thread to stay consistent with the regular way iOS tests are run, and also to avoid
    // Kotlin/Native thread issues when sharing state with the main loop.
    val testRunner = GlobalScope.launch(Dispatchers.Main) {
        try {
            block()
        } catch (e: Throwable) { // we want to catch AssertionErrors as well
            // capture the original stack trace in the cause
            throw UncaughtTestCoroutineException(e)
        }
    }

    // Manually run the main loop (to execute tasks that are queued in the main loop)
    while (!testRunner.isCompleted || NSOperationQueue.mainQueue.operationCount.toInt() > 0) {
        if (getTimeNanos() >= maxTimeNanos) {
            testRunner.cancel("Timed out after $timeout")
            error("Job timed out after $timeout")
        }
        if (testRunner.isCompleted) {
            val remainingOps = NSOperationQueue.mainQueue.operationCount.toInt()
            println("WARN: $remainingOps dangling operation(s) are still in the main queue after the test is completed")
        }
        advanceMainRunLoopFor(seconds = 0.1)
    }
}

private class UncaughtTestCoroutineException(cause: Throwable) : Exception(cause)

/**
 * Runs queued operations from the main loop for the given number of [seconds].
 * This function relies on cooperative tasks, so it won't interrupt tasks that block the main thread for a longer time.
 */
private fun advanceMainRunLoopFor(seconds: Double) {
    NSRunLoop.mainRunLoop.runUntilDate(nowPlusSeconds(seconds))
}

private fun nowPlusSeconds(seconds: Double) = NSDate().addTimeInterval(seconds) as NSDate
