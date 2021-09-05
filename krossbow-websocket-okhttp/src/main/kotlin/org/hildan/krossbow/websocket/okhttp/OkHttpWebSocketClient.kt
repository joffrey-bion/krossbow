package org.hildan.krossbow.websocket.okhttp

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.hildan.krossbow.websocket.WebSocketConnectionException
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketListenerChannelAdapter
import org.hildan.krossbow.websocket.WebSocketClient as KrossbowWebSocketClient
import org.hildan.krossbow.websocket.WebSocketConnection as KrossbowWebSocketSession

class OkHttpWebSocketClient(
    private val client: OkHttpClient = OkHttpClient()
) : KrossbowWebSocketClient {

    override suspend fun connect(url: String): KrossbowWebSocketSession {
        try {
            val request = Request.Builder().url(url).build()
            val channelListener = WebSocketListenerChannelAdapter()
            val okHttpListener = KrossbowToOkHttpListenerAdapter(channelListener)
            val okWebsocket = withContext(Dispatchers.IO) { client.newWebSocket(request, okHttpListener) }
            return OkHttpSocketToKrossbowConnectionAdapter(okWebsocket, channelListener.incomingFrames)
        } catch (e: CancellationException) {
            throw e // this is an upstream exception that we don't want to wrap here
        } catch (e: Exception) {
            throw WebSocketConnectionException(url, cause = e)
        }
    }
}

private class KrossbowToOkHttpListenerAdapter(
    private val channelListener: WebSocketListenerChannelAdapter
) : WebSocketListener() {

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        runBlocking { channelListener.onBinaryMessage(bytes.toByteArray()) }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        runBlocking { channelListener.onTextMessage(text) }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        runBlocking { channelListener.onClose(code, reason) }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        channelListener.onError(t)
    }
}

private class OkHttpSocketToKrossbowConnectionAdapter(
    private val okSocket: WebSocket,
    framesChannel: ReceiveChannel<WebSocketFrame>
) : KrossbowWebSocketSession {

    override val url: String
        get() = okSocket.request().url.toString()

    override val canSend: Boolean
        get() = true // all send methods are just no-ops when the session is closed, so always OK

    override val incomingFrames: ReceiveChannel<WebSocketFrame> = framesChannel

    override suspend fun sendText(frameText: String) {
        okSocket.send(frameText)
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        okSocket.send(frameData.toByteString())
    }

    override suspend fun close(code: Int, reason: String?) {
        okSocket.close(code, reason)
    }
}
