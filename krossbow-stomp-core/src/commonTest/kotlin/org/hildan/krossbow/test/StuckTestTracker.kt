package org.hildan.krossbow.test

import kotlinx.coroutines.CoroutineName
import org.hildan.krossbow.utils.generateUuid
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class StuckTestTracker : AbstractCoroutineContextElement(CoroutineName) {
    /**
     * Key for [StuckTestTracker] instance in the coroutine context.
     */
    companion object Key : CoroutineContext.Key<StuckTestTracker>

    override val key: CoroutineContext.Key<*>
        get() = Key

    val calls: MutableMap<String, String> = mutableMapOf()
}

private val CoroutineContext.tracker: StuckTestTracker
    get() = get(StuckTestTracker.Key) ?: error("No StuckTestTracker in coroutine context")

val CoroutineContext.stuckCalls: Collection<String>
    get() = tracker.calls.values

internal inline fun <T> CoroutineContext.withStuckTracking(callInfo: String, block: () -> T): T {
    val tracker = get(StuckTestTracker.Key) ?: error("Cannot track without StuckTestTracker")
    val trackId = generateUuid()
    tracker.calls[trackId] = callInfo
    val result = block()
    tracker.calls.remove(trackId)
    return result
}
