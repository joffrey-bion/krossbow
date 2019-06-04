package org.hildan.krossbow.testutils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun <T> runAsyncTest(block: suspend () -> T): dynamic = GlobalScope.promise { block() }
