package org.hildan.krossbow.websocket.ktor

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocketSession
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readReason
import io.ktor.http.cio.websocket.readText
import io.ktor.http.takeFrom
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.produceIn
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.WebSocketCloseCodes
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketSession
import kotlin.coroutines.EmptyCoroutineContext

class KtorWebSocketClient @OptIn(KtorExperimentalAPI::class) constructor(
    private val httpClient: HttpClient = HttpClient { install(WebSockets) }
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketSession {
        val wsKtorSession = httpClient.webSocketSession {
            this.url.takeFrom(url)
        }
        return KtorWebSocketSessionAdapter(wsKtorSession)
    }
}

private class KtorWebSocketSessionAdapter(
    private val wsSession: DefaultClientWebSocketSession
) : WebSocketSession {

    private val scope = CoroutineScope(EmptyCoroutineContext + Job() + CoroutineName("krossbow-ktor-ws-frames-mapper"))

    @OptIn(ExperimentalCoroutinesApi::class)
    override val canSend: Boolean
        get() = !wsSession.outgoing.isClosedForSend

    @OptIn(FlowPreview::class)
    override val incomingFrames: ReceiveChannel<WebSocketFrame>
        get() = wsSession.incoming.consumeAsFlow().mapNotNull { it.toKrossbowFrame() }.produceIn(scope)

    override suspend fun sendText(frameText: String) {
        wsSession.outgoing.send(Frame.Text(frameText))
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        wsSession.outgoing.send(Frame.Binary(fin = true, data = frameData))
    }

    override suspend fun close(code: Int, reason: String?) {
        wsSession.close(CloseReason(code.toShort(), reason ?: ""))
        scope.cancel()
    }
}

private fun Frame.toKrossbowFrame(): WebSocketFrame? = when (this) {
    is Frame.Text -> WebSocketFrame.Text(readText())
    is Frame.Binary -> WebSocketFrame.Binary(readBytes())
    is Frame.Close -> toKrossbowCloseFrame()
    else -> null // we ignore Ping/Pong because it's supposed to be handled by the WS implementation
}

private fun Frame.Close.toKrossbowCloseFrame(): WebSocketFrame.Close {
    val reason = readReason()
    val code = reason?.code?.toInt() ?: WebSocketCloseCodes.NO_STATUS_CODE
    return WebSocketFrame.Close(code, reason?.message)
}
