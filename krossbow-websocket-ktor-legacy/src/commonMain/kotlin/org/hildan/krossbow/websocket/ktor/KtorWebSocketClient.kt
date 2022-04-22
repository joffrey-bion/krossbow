package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.WebSocketException

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

    override val url: String
        get() = wsSession.call.request.url.toString()

    @OptIn(ExperimentalCoroutinesApi::class) // for isClosedForSend
    override val canSend: Boolean
        get() = !wsSession.outgoing.isClosedForSend

    private val emittedCloseFrame = atomic(false)

    override val incomingFrames: Flow<WebSocketFrame> =
        wsSession.incoming.receiveAsFlow()
            .map { it.toKrossbowFrame() }
            .onCompletion { error ->
                // Ktor just closes the channel without sending the close frame, so we build it ourselves here.
                // Clients could collect the flow multiple times, which calls onCompletion each time, but we only want
                // to emit the Close frame once, as if it were in the channel like the other frames.
                if (error == null && !emittedCloseFrame.getAndSet(true)) {
                    buildCloseFrame()?.let { emit(it) }
                }
            }
            .catch { th ->
                throw WebSocketException("error in Ktor's websocket: ${th.message}", cause = th)
            }

    private suspend fun buildCloseFrame(): WebSocketFrame.Close? = wsSession.closeReason.await()?.let { reason ->
        WebSocketFrame.Close(reason.code.toInt(), reason.message)
    }

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
