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
import kotlinx.coroutines.selects.*
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
            .onCompletion { cause ->
                // Ktor just closes the incoming channel without sending the close frame, so we build it ourselves.
                // We can only emit from onCompletion when the upstream completed normally (cause == null): when it
                // failed (e.g. the CIO engine cancels the incoming channel on close), onCompletion runs its action on
                // a collector that re-throws the upstream exception on emit, so the Close frame is instead emitted from
                // catch below.
                // Clients could collect the flow multiple times, which calls onCompletion each time, but we only want
                // to emit the Close frame once, as if it were in the channel like the other frames, hence the atomic.
                if (cause == null) {
                    val closeReason = awaitSessionCloseReason()
                    if (closeReason != null && !emittedCloseFrame.getAndSet(true)) {
                        emit(closeReason.toCloseFrame())
                    }
                }
            }
            .catch { th ->
                // Never swallow a genuine cancellation of the collecting coroutine.
                currentCoroutineContext().ensureActive()
                // Some engines (notably CIO on Kotlin/Native) cancel the incoming channel when the connection closes,
                // which surfaces here as a CancellationException rather than a normal completion. If the session
                // recorded a close reason, this is a normal close: emit our synthesized Close frame (once) and let the
                // flow complete normally. Otherwise it's a genuine error and we wrap it.
                val closeReason = awaitSessionCloseReason()
                if (closeReason != null) {
                    if (!emittedCloseFrame.getAndSet(true)) {
                        emit(closeReason.toCloseFrame())
                    }
                } else {
                    throw WebSocketException("error in Ktor's websocket: $th", cause = th)
                }
            }

    /**
     * Awaits the session's [closeReason][DefaultClientWebSocketSession.closeReason] so we can build our artificial
     * Close frame, but stops waiting as soon as the session's coroutine job completes.
     *
     * This is only called once the connection is actually ending (either the incoming channel completed normally, or
     * it failed/was cancelled), so the session job is guaranteed to complete and we never hang. As a safety net, we
     * still bail out immediately if the incoming channel isn't closed yet, so a partial collection (e.g. via
     * `firstOrNull()`) while the connection is still open can never block here.
     *
     * We can't read the close reason eagerly without waiting, because the CIO engine completes it slightly *after* it
     * closes the incoming channel, so a non-suspending read would race ahead of it and miss the Close frame. But we
     * can't await it unconditionally either, because it sometimes never completes (again, notably in the CIO engine),
     * which would hang the flow forever. Racing the await against the session job completion waits just long enough to
     * catch the close reason without risking an infinite wait: the session job always completes once the connection is
     * over, whereas the close reason alone may never do so.
     */
    @OptIn(DelicateCoroutinesApi::class) // for isClosedForReceive
    private suspend fun awaitSessionCloseReason(): CloseReason? {
        if (!wsSession.incoming.isClosedForReceive) {
            return null // the connection is still open, this is just a partial collection of the frames flow
        }
        return select {
            wsSession.closeReason.onAwait { it }
            wsSession.coroutineContext.job.onJoin { wsSession.closeReason.getOrNull() }
        }
    }

    /**
     * Gets this [Deferred]'s value if it has already completed, or `null` otherwise (never wait).
     */
    private suspend fun <T> Deferred<T>.getOrNull(): T? = if (isCompleted) await() else null

    private fun CloseReason.toCloseFrame(): WebSocketFrame.Close = WebSocketFrame.Close(code.toInt(), message)

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
