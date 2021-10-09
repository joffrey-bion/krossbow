package org.hildan.krossbow.websocket.test

internal actual suspend fun runAlongEchoWSServer(
    onOpenActions: ActionsBuilder.() -> Unit,
    block: suspend (port: Int) -> Unit,
) {
    TODO("Implement test WS echo server on JS platform")
}
