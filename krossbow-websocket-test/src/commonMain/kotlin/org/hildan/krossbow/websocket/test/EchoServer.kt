package org.hildan.krossbow.websocket.test

internal expect suspend fun runAlongEchoWSServer(block: suspend (port: Int) -> Unit)
