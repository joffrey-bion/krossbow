package org.hildan.krossbow.engines.mpp.websocket

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocket
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class KtorWebSocket: WebSocket {

    private val client: HttpClient = HttpClient { install(WebSockets) }

    override suspend fun connect(url: String): WebSocketSession {
        val incMessages = Channel<ByteArray>()
        val outFrames = Channel<Frame>()
        val job = GlobalScope.launch {
            client.webSocket(urlString = url) {
                coroutineScope {
                    launch {
                        for (msg in incoming) {
                            incMessages.send(msg.data)
                        }
                    }
                    launch {
                        for (f in outFrames) {
                            outgoing.send(f)
                        }
                    }
                }
            }
        }
        return object : WebSocketSession {

            override val incomingFrames: ReceiveChannel<ByteArray>
                get() = incMessages

            override suspend fun send(frameData: ByteArray) {
                outFrames.send(Frame.Binary(false, frameData))
            }

            override suspend fun close() {
                outFrames.send(Frame.Binary(true, ByteArray(0)))
                job.cancel()
            }
        }
    }
}
