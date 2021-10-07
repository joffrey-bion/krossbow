package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class IgnoreOnNative()

expect fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit)
