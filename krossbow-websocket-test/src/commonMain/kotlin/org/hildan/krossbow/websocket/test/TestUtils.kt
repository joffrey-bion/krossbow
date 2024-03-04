package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

expect fun runSuspendingTest(
    timeout: Duration = 15.seconds,
    block: suspend CoroutineScope.() -> Unit,
)
