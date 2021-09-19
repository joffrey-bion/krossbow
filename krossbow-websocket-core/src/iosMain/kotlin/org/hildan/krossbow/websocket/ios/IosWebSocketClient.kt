package org.hildan.krossbow.websocket.ios

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.hildan.krossbow.websocket.*
import platform.Foundation.*
import platform.darwin.NSObject
import platform.posix.int64_t
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

fun debug(msg: String) {
    println("[ios debug] $msg")
}

class IosWebSocketClient(
    private val config: NSURLSessionConfiguration = NSURLSessionConfiguration.defaultSessionConfiguration(),
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketConnection {
        val socketEndpoint = NSURL.URLWithString(url)!!

        debug("connect - suspending")
        return suspendCancellableCoroutine { continuation ->
            val incomingFrames: Channel<WebSocketFrame> = Channel(BUFFERED)
            val urlSession = NSURLSession.sessionWithConfiguration(
                configuration = config,
                delegate = object : NSObject(), NSURLSessionWebSocketDelegateProtocol {
                    override fun URLSession(
                        session: NSURLSession,
                        webSocketTask: NSURLSessionWebSocketTask,
                        didOpenWithProtocol: String?
                    ) {
                        debug("connect delegate - didOpenWithProtocol - resuming in URLSession")
                        continuation.resume(IosWebSocketConnection(url, incomingFrames, webSocketTask))
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        webSocketTask: NSURLSessionWebSocketTask,
                        didCloseWithCode: NSURLSessionWebSocketCloseCode,
                        reason: NSData?
                    ) {
                        debug("connect delegate - didCloseWithCode - trySend(Close) in URLSession")
                        incomingFrames.trySend(WebSocketFrame.Close(didCloseWithCode.toInt(), reason?.decodeToString()))
                        debug("connect delegate - didCloseWithCode - close() in URLSession")
                        incomingFrames.close()
                    }

                    override fun URLSession(session: NSURLSession, taskIsWaitingForConnectivity: NSURLSessionTask) {
                        debug("connect delegate - taskIsWaitingForConnectivity")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        didCompleteWithError: NSError?
                    ) {
                        debug("connect delegate - didCompleteWithError = ${didCompleteWithError?.localizedDescription}")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        didFinishCollectingMetrics: NSURLSessionTaskMetrics
                    ) {
                        debug("connect delegate - didFinishCollectingMetrics")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        needNewBodyStream: (NSInputStream?) -> Unit
                    ) {
                        debug("connect delegate - needNewBodyStream")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        didReceiveChallenge: NSURLAuthenticationChallenge,
                        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
                    ) {
                        debug("connect delegate - didReceiveChallenge")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        willBeginDelayedRequest: NSURLRequest,
                        completionHandler: (NSURLSessionDelayedRequestDisposition, NSURLRequest?) -> Unit
                    ) {
                        debug("connect delegate - willBeginDelayedRequest")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        didSendBodyData: int64_t,
                        totalBytesSent: int64_t,
                        totalBytesExpectedToSend: int64_t
                    ) {
                        debug("connect delegate - didSendBodyData")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        willPerformHTTPRedirection: NSHTTPURLResponse,
                        newRequest: NSURLRequest,
                        completionHandler: (NSURLRequest?) -> Unit
                    ) {
                        debug("connect delegate - willPerformHTTPRedirection")
                    }

                    override fun URLSession(session: NSURLSession, didBecomeInvalidWithError: NSError?) {
                        debug("connect delegate - didBecomeInvalidWithError = ${didBecomeInvalidWithError?.localizedDescription}")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        didReceiveChallenge: NSURLAuthenticationChallenge,
                        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
                    ) {
                        debug("connect delegate - didReceiveChallenge = $didReceiveChallenge")
                    }

                    override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) {
                        debug("connect delegate - URLSessionDidFinishEventsForBackgroundURLSession")
                    }
                },
                delegateQueue = NSOperationQueue.currentQueue()
            )
            debug("connect - urlSession created, calling webSocketTaskWithURL")
            val webSocket = urlSession.webSocketTaskWithURL(socketEndpoint)
            debug("connect - got websocket, calling forwardIncomingMessagesAsyncTo")
            webSocket.forwardIncomingMessagesAsyncTo(incomingFrames)
            debug("connect - calling webSocket.resume()")
            webSocket.resume()
            debug("connect - done in suspendCoroutine")
            continuation.invokeOnCancellation {
                debug("connect - invokeOnCancellation - cancelling websocket")
                webSocket.cancel()
            }
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
        debug("IosWebSocketConnection - sendText - frameText = $frameText")
        sendMessage(NSURLSessionWebSocketMessage(frameText))
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        debug("IosWebSocketConnection - sendBinary - frameData.size = ${frameData.size}")
        sendMessage(NSURLSessionWebSocketMessage(frameData.toNSData()))
    }

    private suspend fun sendMessage(message: NSURLSessionWebSocketMessage) {
        suspendCoroutine<Unit> { cont ->
            webSocket.sendMessage(message) { err ->
                debug("IosWebSocketConnection - sendMessage - callback with err = ${err?.localizedDescription}")
                when (err) {
                    null -> cont.resume(Unit)
                    else -> cont.resumeWithException(WebSocketException(err.localizedDescription))
                }
            }
        }
    }

    override suspend fun close(code: Int, reason: String?) {
        debug("IosWebSocketConnection - close")
        canSend = false
        webSocket.cancelWithCloseCode(code.toLong(), reason?.encodeToNSData())
    }
}

private fun NSURLSessionWebSocketTask.forwardIncomingMessagesAsyncTo(incomingFrames: SendChannel<WebSocketFrame>) {
    debug("forwardIncomingMessagesAsyncTo - calling receiveMessageWithCompletionHandler")
    receiveMessageWithCompletionHandler { message, nsError ->
        debug("forwardIncomingMessagesAsyncTo - callback!")
        when {
            nsError != null -> {
                debug("forwardIncomingMessagesAsyncTo - closing with error (nsError = ${nsError.localizedDescription})")
                incomingFrames.close(WebSocketException(nsError.localizedDescription))
            }
            message != null -> {
                debug("forwardIncomingMessagesAsyncTo - received message (message = ${message.toWebSocketFrame()})")
                incomingFrames.trySend(message.toWebSocketFrame())
                forwardIncomingMessagesAsyncTo(incomingFrames)
            }
        }
    }
}

private fun NSURLSessionWebSocketMessage.toWebSocketFrame(): WebSocketFrame = when (type) {
    NSURLSessionWebSocketMessageTypeData -> WebSocketFrame.Binary(
        bytes = data?.toByteArray() ?: error("Message of type NSURLSessionWebSocketMessageTypeData has null value for 'data'")
    )
    NSURLSessionWebSocketMessageTypeString -> WebSocketFrame.Text(
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
    // length=0 breaks the code below for some reason (ArrayIndexOutOfBoundsException)
    // and it doesn't hurt to shortcut memcpy anyway if the array is empty
    if (length.toInt() == 0) return ByteArray(0)

    val data = this
    return ByteArray(data.length.toInt()).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
    }
}
