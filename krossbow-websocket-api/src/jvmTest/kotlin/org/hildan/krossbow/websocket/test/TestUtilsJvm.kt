package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.runBlocking

actual fun runSuspendingTest(testBlock: suspend () -> Unit) = runBlocking { testBlock() }
