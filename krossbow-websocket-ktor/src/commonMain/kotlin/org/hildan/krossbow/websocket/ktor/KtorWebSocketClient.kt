package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketCloseCodes
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketConnectionWithPingPong
import kotlin.coroutines.EmptyCoroutineContext

class KtorWebSocketClient @OptIn(KtorExperimentalAPI::class) constructor(
    private val httpClient: HttpClient = HttpClient { install(WebSockets) }
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketConnectionWithPingPong {
        val wsKtorSession = httpClient.webSocketSession {
            this.url.takeFrom(url)
        }
        return KtorWebSocketConnectionAdapter(wsKtorSession)
    }
}

private class KtorWebSocketConnectionAdapter(
    private val wsSession: DefaultClientWebSocketSession
) : WebSocketConnectionWithPingPong {

    private val scope = CoroutineScope(EmptyCoroutineContext + Job() + CoroutineName("krossbow-ktor-ws-frames-mapper"))

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
