package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import org.hildan.krossbow.websocket.*

class KtorWebSocketClient(
    private val httpClient: HttpClient = HttpClient { install(WebSockets) }
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketConnectionWithPingPong {
        try {
            val wsKtorSession = httpClient.webSocketSession {
                this.url.takeFrom(url)
            }
            return KtorWebSocketConnectionAdapter(wsKtorSession)
        } catch (e: CancellationException) {
            throw e // this is an upstream exception that we don't want to wrap here
        } catch (e: Exception) {
            throw WebSocketConnectionException(url, cause = e)
        }
    }
}

private class KtorWebSocketConnectionAdapter(
    private val wsSession: DefaultClientWebSocketSession
) : WebSocketConnectionWithPingPong {

    private val scope = CoroutineScope(CoroutineName("krossbow-ktor-ws-frames-mapper"))

    override val url: String
        get() = wsSession.call.request.url.toString()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val canSend: Boolean
        get() = !wsSession.outgoing.isClosedForSend

    @OptIn(FlowPreview::class)
    override val incomingFrames: ReceiveChannel<WebSocketFrame>
        get() = wsSession.incoming.consumeAsFlow().map { it.toKrossbowFrame() }.produceIn(scope)

    override suspend fun sendText(frameText: String) {
        wsSession.outgoing.send(Frame.Text(frameText))
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        wsSession.outgoing.send(Frame.Binary(fin = true, data = frameData))
    }

    override suspend fun sendPing(frameData: ByteArray) {
        wsSession.outgoing.send(Frame.Ping(frameData))
    }

    override suspend fun sendPong(frameData: ByteArray) {
        wsSession.outgoing.send(Frame.Pong(frameData))
    }

    override suspend fun close(code: Int, reason: String?) {
        wsSession.close(CloseReason(code.toShort(), reason ?: ""))
        scope.cancel()
    }
}

private fun Frame.toKrossbowFrame(): WebSocketFrame = when (this) {
    is Frame.Text -> WebSocketFrame.Text(readText())
    is Frame.Binary -> WebSocketFrame.Binary(readBytes())
    is Frame.Ping -> WebSocketFrame.Ping(readBytes())
    is Frame.Pong -> WebSocketFrame.Pong(readBytes())
    is Frame.Close -> toKrossbowCloseFrame()
    else -> error("Unknown frame type ${this::class.simpleName}")
}

private fun Frame.Close.toKrossbowCloseFrame(): WebSocketFrame.Close {
    val reason = readReason()
    val code = reason?.code?.toInt() ?: WebSocketCloseCodes.NO_STATUS_CODE
    return WebSocketFrame.Close(code, reason?.message)
}
