package org.hildan.krossbow.websocket.test

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.Foundation.*
import kotlin.coroutines.CoroutineContext
import kotlin.test.fail
import kotlin.time.*
import kotlin.time.Duration.Companion.milliseconds

actual fun runSuspendingTest(timeoutMillis: Long, block: suspend CoroutineScope.() -> Unit) =
    runBlockingButRunningMainLoop(timeoutMillis.milliseconds) { block() }

/**
 * Runs a new coroutine and blocks the current thread interruptibly until its completion.
 *
 * While the thread is blocked from the outside, this method still runs the iOS main loop periodically concurrently
 * with the given [block] to make sure every queued operation (in [NSOperationQueue.mainQueue]) can progress.
 *
 * Inspired by https://github.com/ktorio/ktor/issues/678#issuecomment-433756753
 */
@OptIn(UnsafeNumber::class)
fun runBlockingButRunningMainLoop(timeout: Duration, block: suspend CoroutineScope.() -> Unit) {
    val maxTimeMark = TimeSource.Monotonic.markNow() + timeout
    // runBlocking is required in order to start an event loop (cannot run anything from the main loop without it)
    runBlocking {
        var blockIsDone = false

        // We start the given block inside a concurrent coroutine, so we can ensure we run stuff from the main run loop
        // at the same time (to handle queued callbacks etc.).
        // Running in the MainRunLoopDispatcher allows more predictability for the intertwining of the queued tasks
        // and this coroutine's "parts" between suspension points.
        // We also need to make sure we run on the main thread (otherwise we can get "illegal attempt to access
        // non-shared object from other thread" when tasks are run on the main run loop), so MainRunLoopDispatcher
        // ensures that as well.
        // However, this kind of errors only ever happened when using GlobalScope (and thus the Default dispatcher)
        // so it might not be needed.
        val testRunner = launch(MainRunLoopDispatcher) {
            try {
                block()
                blockIsDone = true
            } catch (e: Throwable) { // we want to catch AssertionErrors as well
                // capture the original stack trace in the cause
                throw UncaughtTestCoroutineException(e)
            }
        }

        // Wait until the block is finished (for the runBlocking semantics), but still run the main loop in the
        // meantime to perform the enqueued tasks, otherwise we could hang forever if the block() awaits callbacks
        // that are run in the main operation queue.
        // This also waits for remaining tasks run with NSOperationQueue.mainQueue.addOperationWithBlock{...} even
        // after block() is done, to provide some kind of "wait for children" semantics. Note that this could miss code
        // that is run with NSRunLoop.mainRunLoop().performBlock{...} because it's not part of the queue.
        // We cannot do the above by calling NSOperationQueue.mainQueue.waitUntilAllOperationsAreFinished() because
        // this would just block the main thread but wouldn't actually run those operations. That's why we still need
        // this "advance and check" loop when processing the remaining queued operations.
        while (!blockIsDone || NSOperationQueue.mainQueue.operationCount.toInt() > 0) {
            if (maxTimeMark.hasPassedNow()) {
                testRunner.cancel("Timed out after $timeout")
                fail("Test timed out after $timeout")
            }
            if (testRunner.isCompleted) {
                val remainingOps = NSOperationQueue.mainQueue.operationCount.toInt()
                println("WARN: $remainingOps dangling operation(s) are still in the main queue after the test is completed")
            }

            // manually run the main loop (to execute parts of the block() and queued tasks)
            advanceMainRunLoopFor(seconds = 0.1)

            // allow the MainRunLoopDispatcher to progress coroutines (even though this dispatcher runs the pieces of
            // code between suspension points on the main loop, it still needs to get the thread back in order to enqueue
            // the code to the main loop)
            yield()
        }
    }
}

private class UncaughtTestCoroutineException(cause: Throwable) : Exception(cause)

/**
 * A dispatcher that runs code in the main run loop.
 * In tests, this main run loop needs to be executed manually (for instance with [advanceMainRunLoopFor]).
 */
private object MainRunLoopDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        NSRunLoop.mainRunLoop().performBlock {
            block.run()
        }
    }
}

/**
 * Runs queued operations from the main loop for the given number of [seconds].
 * This function relies on cooperative tasks, so it won't interrupt tasks that block the main thread for a longer time.
 */
private fun advanceMainRunLoopFor(seconds: Double) {
    NSRunLoop.mainRunLoop.runUntilDate(nowPlusSeconds(seconds))
}

private fun nowPlusSeconds(seconds: Double) = NSDate().addTimeInterval(seconds) as NSDate
