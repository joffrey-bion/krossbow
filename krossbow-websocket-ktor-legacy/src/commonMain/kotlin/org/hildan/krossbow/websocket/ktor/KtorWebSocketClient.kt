package org.hildan.krossbow.websocket.ktor

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.io.bytestring.*
import kotlinx.io.bytestring.unsafe.*
import org.hildan.krossbow.io.*
import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.WebSocketException

class KtorWebSocketClient(
    private val httpClient: HttpClient = HttpClient { install(WebSockets) }
) : WebSocketClient {

    override val supportsCustomHeaders: Boolean = !PlatformUtils.IS_BROWSER

    override suspend fun connect(url: String, protocols: List<String>, headers: Map<String, String>): WebSocketConnectionWithPingPong {
        require(headers.isEmpty() || supportsCustomHeaders) {
            "Custom web socket handshake headers are not supported in this Ktor engine " +
                "(${httpClient.engine::class.simpleName}) on this platform (${PlatformUtils.platform})"
        }
        try {
            val wsKtorSession = httpClient.webSocketSession(url) {
                // Ktor doesn't support comma-separated protocols in a single header, so we send a repeated header 
                // instead (see https://youtrack.jetbrains.com/issue/KTOR-6971)
                protocols.forEach {
                    header(HttpHeaders.SecWebSocketProtocol, it)
                }
                headers.forEach { (name, value) ->
                    header(name, value)
                }
            }
            return KtorWebSocketConnectionAdapter(wsKtorSession)
        } catch (e: CancellationException) {
            throw e // this is an upstream exception that we don't want to wrap here
        } catch (e: ResponseException) {
            throw WebSocketConnectionException(url, httpStatusCode = e.response.status.value, cause = e)
        } catch (e: Exception) {
            val (statusCode, additionalInfo) = extractKtorHandshakeFailureDetails(e)
            throw WebSocketConnectionException(url, httpStatusCode = statusCode, additionalInfo = additionalInfo, cause = e)
        }
    }
}

private class KtorWebSocketConnectionAdapter(
    private val wsSession: DefaultClientWebSocketSession
) : WebSocketConnectionWithPingPong {

    override val url: String = wsSession.call.request.url.toString()

    override val protocol: String? = wsSession.call.response.headers[HttpHeaders.SecWebSocketProtocol]
    
    @OptIn(DelicateCoroutinesApi::class) // for isClosedForSend
    override val canSend: Boolean
        get() = !wsSession.outgoing.isClosedForSend

    private val emittedCloseFrame = atomic(false)

    override val incomingFrames: Flow<WebSocketFrame> =
        wsSession.incoming.receiveAsFlow()
            .map { it.toKrossbowFrame() }
            .onEach {
                // We don't need our fake Close frame if there is one (in JS engine it seems there is)
                if (it is WebSocketFrame.Close) {
                    emittedCloseFrame.getAndSet(true)
                }
            }
            .onCompletion { error ->
                // Ktor just closes the channel without sending the close frame, so we build it ourselves here.
                // Clients could collect the flow multiple times, which calls onCompletion each time, but we only want
                // to emit the Close frame once, as if it were in the channel like the other frames.
                if (error == null && !emittedCloseFrame.getAndSet(true)) {
                    buildCloseFrame()?.let { emit(it) }
                }
            }
            .catch { th ->
                throw WebSocketException("error in Ktor's websocket: $th", cause = th)
            }

    private suspend fun buildCloseFrame(): WebSocketFrame.Close? = wsSession.closeReason.await()?.let { reason ->
        WebSocketFrame.Close(reason.code.toInt(), reason.message)
    }

    override suspend fun sendText(frameText: String) {
        wsSession.outgoing.send(Frame.Text(frameText))
    }

    @OptIn(UnsafeByteStringApi::class)
    override suspend fun sendBinary(frameData: ByteString) {
        wsSession.outgoing.send(Frame.Binary(fin = true, data = frameData.unsafeBackingByteArray()))
    }

    @OptIn(UnsafeByteStringApi::class)
    override suspend fun sendPing(frameData: ByteString) {
        wsSession.outgoing.send(Frame.Ping(frameData.unsafeBackingByteArray()))
    }

    @OptIn(UnsafeByteStringApi::class)
    override suspend fun sendPong(frameData: ByteString) {
        wsSession.outgoing.send(Frame.Pong(frameData.unsafeBackingByteArray()))
    }

    override suspend fun close(code: Int, reason: String?) {
        wsSession.close(CloseReason(code.toShort(), reason ?: ""))
    }
}

@OptIn(UnsafeByteStringApi::class)
private fun Frame.toKrossbowFrame(): WebSocketFrame = when (this) {
    is Frame.Text -> WebSocketFrame.Text(readText())
    is Frame.Binary -> WebSocketFrame.Binary(readBytes().asByteString())
    is Frame.Ping -> WebSocketFrame.Ping(readBytes().asByteString())
    is Frame.Pong -> WebSocketFrame.Pong(readBytes().asByteString())
    is Frame.Close -> toKrossbowCloseFrame()
    else -> error("Unknown frame type ${this::class.simpleName}")
}

private fun Frame.Close.toKrossbowCloseFrame(): WebSocketFrame.Close {
    val reason = readReason()
    val code = reason?.code?.toInt() ?: WebSocketCloseCodes.NO_STATUS_CODE
    return WebSocketFrame.Close(code, reason?.message)
}
