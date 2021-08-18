package org.hildan.krossbow.websocket.ios

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import org.hildan.krossbow.websocket.*
import platform.Foundation.*
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class IosWebSocketClient(
    private val config: NSURLSessionConfiguration = NSURLSessionConfiguration.defaultSessionConfiguration(),
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketConnection {
        val socketEndpoint = NSURL.URLWithString(url)!!

        return suspendCoroutine { continuation ->
            val incomingFrames: Channel<WebSocketFrame> = Channel(BUFFERED)
            val urlSession = NSURLSession.sessionWithConfiguration(
                configuration = config,
                delegate = object : NSObject(), NSURLSessionWebSocketDelegateProtocol {
                    override fun URLSession(
                        session: NSURLSession,
                        webSocketTask: NSURLSessionWebSocketTask,
                        didOpenWithProtocol: String?
                    ) {
                        continuation.resume(IosWebSocketConnection(url, incomingFrames, webSocketTask))
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        webSocketTask: NSURLSessionWebSocketTask,
                        didCloseWithCode: NSURLSessionWebSocketCloseCode,
                        reason: NSData?
                    ) {
                        runBlocking {
                            incomingFrames.send(WebSocketFrame.Close(didCloseWithCode.toInt(), reason?.decodeToString()))
                            incomingFrames.close()
                        }
                    }
                },
                delegateQueue = NSOperationQueue.currentQueue()
            )
            val webSocket = urlSession.webSocketTaskWithURL(socketEndpoint)
            webSocket.forwardIncomingMessagesAsyncTo(incomingFrames)
            webSocket.resume()
        }
    }
}

class IosWebSocketConnection(
    override val url: String,
    override val incomingFrames: ReceiveChannel<WebSocketFrame>,
    private val webSocket: NSURLSessionWebSocketTask,
) : WebSocketConnection {

    // no clear way to know if the websocket was closed by the peer
    override var canSend: Boolean = true

    override suspend fun sendText(frameText: String) {
        sendMessage(NSURLSessionWebSocketMessage(frameText))
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        sendMessage(NSURLSessionWebSocketMessage(frameData.toNSData()))
    }

    private suspend fun sendMessage(message: NSURLSessionWebSocketMessage) {
        suspendCoroutine<Unit> { cont ->
            webSocket.sendMessage(message) { err ->
                when (err) {
                    null -> cont.resume(Unit)
                    else -> cont.resumeWithException(WebSocketException(err.localizedDescription))
                }
            }
        }
    }

    override suspend fun close(code: Int, reason: String?) {
        canSend = false
        webSocket.cancelWithCloseCode(code.toLong(), reason?.encodeToNSData())
    }
}

private fun NSURLSessionWebSocketTask.forwardIncomingMessagesAsyncTo(incomingFrames: SendChannel<WebSocketFrame>) {
    receiveMessageWithCompletionHandler { message, nsError ->
        when {
            nsError != null -> {
                incomingFrames.close(WebSocketException(nsError.localizedDescription))
            }
            message != null -> runBlocking {
                incomingFrames.send(message.toWebSocketFrame())
                forwardIncomingMessagesAsyncTo(incomingFrames)
            }
        }
    }
}

private fun NSURLSessionWebSocketMessage.toWebSocketFrame(): WebSocketFrame = when (type) {
    NSURLSessionWebSocketMessageTypeData -> org.hildan.krossbow.websocket.WebSocketFrame.Binary(
        bytes = data?.toByteArray() ?: error("Message of type NSURLSessionWebSocketMessageTypeData has null value for 'data'")
    )
    NSURLSessionWebSocketMessageTypeString -> org.hildan.krossbow.websocket.WebSocketFrame.Text(
        text = string ?: error("Message of type NSURLSessionWebSocketMessageTypeString has null value for 'string'")
    )
    else -> error("Unknown NSURLSessionWebSocketMessage type: $type")
}

@Suppress("CAST_NEVER_SUCCEEDS")
private fun String.encodeToNSData(): NSData? = (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)

private fun NSData.decodeToString(): String = toByteArray().decodeToString()

private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
}

private fun NSData.toByteArray(): ByteArray {
    val data = this
    return ByteArray(data.length.toInt()).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
    }
}
