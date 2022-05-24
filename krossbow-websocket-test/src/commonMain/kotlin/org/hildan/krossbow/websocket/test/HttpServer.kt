package org.hildan.krossbow.websocket.test

internal expect suspend fun runAlongHttpServer(block: suspend (baseUrl: String) -> Unit)
