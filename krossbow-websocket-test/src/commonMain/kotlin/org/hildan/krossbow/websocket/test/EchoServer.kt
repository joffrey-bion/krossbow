package org.hildan.krossbow.websocket.test

internal expect suspend fun runAlongEchoWSServer(
    onOpenActions: ActionsBuilder.() -> Unit = {},
    block: suspend (port: Int) -> Unit,
)

internal sealed class ServerAction {
    data class SendTextFrame(val message: String): ServerAction()
    class SendBinaryFrame(val data: ByteArray): ServerAction()
    data class Close(val code: Int, val reason: String?): ServerAction()
}

internal class ActionsBuilder {
    private val actions = mutableListOf<ServerAction>()
    fun sendText(message: String) = actions.add(ServerAction.SendTextFrame(message))
    fun sendBinary(data: ByteArray) = actions.add(ServerAction.SendBinaryFrame(data))
    fun close(code: Int = 1000, reason: String? = null) = actions.add(ServerAction.Close(code, reason))
    fun build(): List<ServerAction> = actions
}
