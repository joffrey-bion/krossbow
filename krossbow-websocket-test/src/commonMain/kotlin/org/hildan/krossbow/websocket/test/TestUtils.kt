package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class IgnoreOnNative()

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class IgnoreOnJS()

@OptIn(ExperimentalTime::class)
expect fun runSuspendingTest(
    timeout: Duration = Duration.seconds(20),
    block: suspend CoroutineScope.() -> Unit,
)
