package org.hildan.krossbow.websocket.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

@OptIn(DelicateCoroutinesApi::class)
actual fun runSuspendingTest(block: suspend CoroutineScope.() -> Unit): dynamic = GlobalScope.promise { block() }

fun isBrowser() = js("typeof window !== 'undefined' && typeof window.document !== 'undefined'") as Boolean
