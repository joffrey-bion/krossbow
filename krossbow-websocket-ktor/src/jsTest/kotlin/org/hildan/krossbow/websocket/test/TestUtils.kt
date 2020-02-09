package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun runSuspendingTest(testBlock: suspend () -> Unit): dynamic = GlobalScope.promise { testBlock() }
