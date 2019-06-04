package org.hildan.krossbow.testutils

expect fun <T> runAsyncTest(block: suspend () -> T)
