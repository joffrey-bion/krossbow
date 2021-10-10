package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class IgnoreOnNative()

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class IgnoreOnJS()

expect fun runSuspendingTest(
    timeoutMillis: Long = 20_000,
    block: suspend CoroutineScope.() -> Unit,
)
