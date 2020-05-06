package org.hildan.krossbow.websocket.okhttp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.hildan.krossbow.websocket.WebSocketFrame
import org.hildan.krossbow.websocket.WebSocketListenerChannelAdapter
import kotlin.coroutines.CoroutineContext
import org.hildan.krossbow.websocket.WebSocketClient as KrossbowWebSocketClient
import org.hildan.krossbow.websocket.WebSocketSession as KrossbowWebSocketSession

class OkHttpWebSocketClient(
    private val client: OkHttpClient = OkHttpClient()
) : KrossbowWebSocketClient {

    override suspend fun connect(url: String): KrossbowWebSocketSession {
        val request = Request.Builder().url(url).build()
        val listener = KrossbowToOkHttpListenerAdapter()
        val okWebsocket = withContext(Dispatchers.IO) { client.newWebSocket(request, listener) }
        return OkHttpSocketToKrossbowSessionAdapter(okWebsocket, listener)
    }
}

private class KrossbowToOkHttpListenerAdapter : WebSocketListener(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job

    val channelListener: WebSocketListenerChannelAdapter = WebSocketListenerChannelAdapter()

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        launch { channelListener.onBinaryMessage(bytes.toByteArray()) }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        launch { channelListener.onTextMessage(text) }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        launch { channelListener.onClose(code, reason) }
        job.cancel()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        channelListener.onError(t)
        job.cancel()
    }

    suspend fun stop() {
        job.cancelAndJoin()
    }
}

private class OkHttpSocketToKrossbowSessionAdapter(
    private val okSocket: WebSocket,
    private val listener: KrossbowToOkHttpListenerAdapter
) : KrossbowWebSocketSession {

    override val canSend: Boolean
        get() = true // all send methods are just no-ops when the session is closed, so always OK

    override val incomingFrames: ReceiveChannel<WebSocketFrame> = listener.channelListener.incomingFrames

    override suspend fun sendText(frameText: String) {
        okSocket.send(frameText)
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        okSocket.send(frameData.toByteString())
    }

    override suspend fun close(code: Int, reason: String?) {
        okSocket.close(code, reason)
        listener.stop()
    }
}
