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
import kotlin.random.Random

fun debugMsg(msg: String) {
    println("[ios debug] $msg")
}

class IosWebSocketClient(
    private val config: NSURLSessionConfiguration = NSURLSessionConfiguration.defaultSessionConfiguration(),
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketConnection {
        println("\n\n[ios debug] Start connect to $url")
        val socketEndpoint = NSURL.URLWithString(url)!!

        // FIXME remove this, it's just for debugging
        val rand = Random.nextInt(100)
        fun debug(msg: String) {
            debugMsg("[$rand] $msg")
        }

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
                        debug("    delegate - didOpenWithProtocol - resuming in URLSession")
                        continuation.resume(IosWebSocketConnection(url, incomingFrames, webSocketTask))
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        webSocketTask: NSURLSessionWebSocketTask,
                        didCloseWithCode: NSURLSessionWebSocketCloseCode,
                        reason: NSData?
                    ) {
                        debug("    delegate - didCloseWithCode - trySend(closeFrame) in URLSession")
                        val closeFrame = WebSocketFrame.Close(didCloseWithCode.toInt(), reason?.decodeToString())
                        val closeResult = incomingFrames.trySend(closeFrame)
                        closeResult.getOrThrow() // TODO better error handling?
                        debug("    delegate - didCloseWithCode - close() in URLSession")
                        incomingFrames.close()
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        didCompleteWithError: NSError?
                    ) {
                        debug("    delegate - didCompleteWithError: $didCompleteWithError")
                        // TODO resumeWithException if still in connecting phase?

                        if (didCompleteWithError != null) {
                            // TODO is this special case normal? We shouldn't really invent CLOSE frames...
                            //  Investigate why sometimes we don't get didCloseWithCode but get this instead
                            if (didCompleteWithError.code.toInt() == 57) {
                                debug("    delegate - didCompleteWithError - simulating CLOSE frame")
                                incomingFrames.trySend(WebSocketFrame.Close(WebSocketCloseCodes.NO_STATUS_CODE, null))
                                incomingFrames.close()
                            } else {
                                incomingFrames.close(didCompleteWithError.toWebSocketException())
                            }
                        }
                    }

                    override fun URLSession(session: NSURLSession, taskIsWaitingForConnectivity: NSURLSessionTask) {
                        debug("    delegate - taskIsWaitingForConnectivity")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        didFinishCollectingMetrics: NSURLSessionTaskMetrics
                    ) {
                        debug("    delegate - didFinishCollectingMetrics")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        needNewBodyStream: (NSInputStream?) -> Unit
                    ) {
                        debug("    delegate - needNewBodyStream")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        didReceiveChallenge: NSURLAuthenticationChallenge,
                        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
                    ) {
                        debug("    delegate - didReceiveChallenge")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        willBeginDelayedRequest: NSURLRequest,
                        completionHandler: (NSURLSessionDelayedRequestDisposition, NSURLRequest?) -> Unit
                    ) {
                        debug("    delegate - willBeginDelayedRequest")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        didSendBodyData: int64_t,
                        totalBytesSent: int64_t,
                        totalBytesExpectedToSend: int64_t
                    ) {
                        debug("    delegate - didSendBodyData")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        task: NSURLSessionTask,
                        willPerformHTTPRedirection: NSHTTPURLResponse,
                        newRequest: NSURLRequest,
                        completionHandler: (NSURLRequest?) -> Unit
                    ) {
                        debug("    delegate - willPerformHTTPRedirection")
                    }

                    override fun URLSession(session: NSURLSession, didBecomeInvalidWithError: NSError?) {
                        debug("    delegate - didBecomeInvalidWithError: $didBecomeInvalidWithError")
                    }

                    override fun URLSession(
                        session: NSURLSession,
                        didReceiveChallenge: NSURLAuthenticationChallenge,
                        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
                    ) {
                        debug("    delegate - didReceiveChallenge: $didReceiveChallenge")
                    }

                    override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) {
                        debug("    delegate - URLSessionDidFinishEventsForBackgroundURLSession")
                    }
                },
                delegateQueue = NSOperationQueue.currentQueue()
            )
            val webSocket = urlSession.webSocketTaskWithURL(socketEndpoint)
            webSocket.forwardNextIncomingMessagesAsyncTo(incomingFrames)
            debug("connect - calling webSocket.resume()")
            webSocket.resume()
            continuation.invokeOnCancellation {
                debug("connect - invokeOnCancellation - cancelling websocket")
                webSocket.cancel()
            }
            debug("connect - done in suspendCoroutine")
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
                debugMsg("IosWebSocketConnection - sendMessage - callback with err = ${err?.localizedDescription}")
                when (err) {
                    null -> cont.resume(Unit)
                    else -> cont.resumeWithException(WebSocketException(err.localizedDescription))
                }
            }
        }
    }

    override suspend fun close(code: Int, reason: String?) {
        debugMsg("IosWebSocketConnection - close")
        canSend = false
        webSocket.cancelWithCloseCode(code.toLong(), reason?.encodeToNSData())
    }
}

private fun NSURLSessionWebSocketTask.forwardNextIncomingMessagesAsyncTo(incomingFrames: SendChannel<WebSocketFrame>) {
    debugMsg("forwardNextIncomingMessagesAsyncTo - calling receiveMessageWithCompletionHandler")
    receiveMessageWithCompletionHandler { message, nsError ->
        when {
            nsError != null -> {
                // TODO it seems the callback is called with error when the websocket is closed normally
                //  Domain=NSPOSIXErrorDomain Code=57 "Socket is not connected"
                if (nsError.code.toInt() != 57) {
                    debugMsg("    MESSAGE - nsError = $nsError")
                    incomingFrames.close(nsError.toWebSocketException())
                } else {
                    debugMsg("    Stopping incoming message polling")
                }
            }
            message != null -> {
                debugMsg("    MESSAGE - frame = ${message.toWebSocketFrame()}")
                incomingFrames.trySend(message.toWebSocketFrame())
                forwardNextIncomingMessagesAsyncTo(incomingFrames)
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

private fun NSError.toWebSocketException() = WebSocketException(localizedDescription)
