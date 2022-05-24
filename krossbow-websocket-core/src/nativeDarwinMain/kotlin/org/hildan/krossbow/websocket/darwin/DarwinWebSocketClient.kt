@file:OptIn(UnsafeNumber::class)

package org.hildan.krossbow.websocket.darwin

import kotlinx.cinterop.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.hildan.krossbow.websocket.*
import platform.Foundation.*
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Error code received in some callbacks when the connection is actually just closed normally.
 */
private const val ERROR_CODE_SOCKET_NOT_CONNECTED = 57

/**
 * An implementation of [WebSocketClient] using darwin's native [NSURLSessionWebSocketTask].
 * This is only available is iOS 13.0+, tvOS 13.0+, watchOS 6.0+, macOS 10.15+
 * (see [documentation](https://developer.apple.com/documentation/foundation/urlsessionwebsockettask))
 *
 * A custom [sessionConfig] can be passed to customize the behaviour of the connection.
 * Also, if a non-null [maximumMessageSize] if provided, it will be used to configure the web socket.
 */
class DarwinWebSocketClient(
    private val sessionConfig: NSURLSessionConfiguration = NSURLSessionConfiguration.defaultSessionConfiguration(),
    private val maximumMessageSize: Long? = null,
) : WebSocketClient {

    override suspend fun connect(url: String): WebSocketConnectionWithPing {
        val socketEndpoint = NSURL.URLWithString(url)!!

        return suspendCancellableCoroutine { cont ->
            val incomingFrames: Channel<WebSocketFrame> = Channel(BUFFERED)
            val urlSession = NSURLSession.sessionWithConfiguration(
                configuration = sessionConfig,
                delegate = IosWebSocketListener(url, cont, incomingFrames),
                delegateQueue = NSOperationQueue.currentQueue()
            )
            val webSocket = urlSession.webSocketTaskWithURL(socketEndpoint)
            maximumMessageSize?.let { webSocket.setMaximumMessageSize(it.convert()) }
            webSocket.forwardNextIncomingMessagesAsyncTo(incomingFrames)
            webSocket.resume()
            cont.invokeOnCancellation {
                webSocket.cancel()
            }
        }
    }
}

private class IosWebSocketListener(
    private val url: String,
    private var connectionContinuation: Continuation<IosWebSocketConnection>?,
    private val incomingFrames: Channel<WebSocketFrame>,
) : NSObject(), NSURLSessionWebSocketDelegateProtocol {
    private var isConnecting = true

    private inline fun completeConnection(resume: Continuation<IosWebSocketConnection>.() -> Unit) {
        val cont = connectionContinuation ?: error("web socket connection continuation already consumed")
        connectionContinuation = null // avoid leaking the continuation
        isConnecting = false
        cont.resume()
    }

    override fun URLSession(session: NSURLSession, webSocketTask: NSURLSessionWebSocketTask, didOpenWithProtocol: String?) {
        completeConnection {
            resume(IosWebSocketConnection(url, incomingFrames.receiveAsFlow(), webSocketTask))
        }
    }

    override fun URLSession(
        session: NSURLSession,
        webSocketTask: NSURLSessionWebSocketTask,
        didCloseWithCode: NSURLSessionWebSocketCloseCode,
        reason: NSData?
    ) {
        passCloseFrameThroughChannel(didCloseWithCode.toInt(), reason?.decodeToString())
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?
    ) {
        if (isConnecting) {
            val ex =
                WebSocketConnectionException(url, cause = didCompleteWithError?.toWebSocketHandshakeException(task.response))
            completeConnection {
                resumeWithException(ex)
            }
            return
        }

        // The error is null in case of server-side errors
        if (didCompleteWithError == null) {
            incomingFrames.close(WebSocketException("NSURLSession failed with unknown server-side error"))
            return
        }

        // For some reason, sometimes we get this error 57 "Socket is closed" instead of didCloseWithCode callback
        if (didCompleteWithError.code.toInt() == ERROR_CODE_SOCKET_NOT_CONNECTED) {
            passCloseFrameThroughChannel(
                code = WebSocketCloseCodes.NO_STATUS_CODE,
                reason = "fake CLOSE frame - got error 57 'Socket is closed' on NSURLSession",
            )
            return
        }

        incomingFrames.close(didCompleteWithError.toIosWebSocketException())
    }

    private fun passCloseFrameThroughChannel(code: Int, reason: String?) {
        val closeResult = incomingFrames.trySend(WebSocketFrame.Close(code, reason))
        if (closeResult.isFailure) {
            val closeException = WebSocketException("Could not pass CLOSE frame through channel", cause = closeResult.exceptionOrNull())
            incomingFrames.close(closeException)
            // still throw because no one might be listening to this channel (especially since the buffer is likely full)
            throw closeException
        }
        incomingFrames.close()
    }
}
private class IosWebSocketConnection(
    override val url: String,
    override val incomingFrames: Flow<WebSocketFrame>,
    private val webSocket: NSURLSessionWebSocketTask,
) : WebSocketConnectionWithPing {

    // no clear way to know if the websocket was closed by the peer, and we can't even fail in sendMessage reliably
    override val canSend: Boolean = true

    override suspend fun sendText(frameText: String) {
        sendMessage(NSURLSessionWebSocketMessage(frameText))
    }

    override suspend fun sendBinary(frameData: ByteArray) {
        sendMessage(NSURLSessionWebSocketMessage(frameData.toNSData()))
    }

    private fun sendMessage(message: NSURLSessionWebSocketMessage) {
        // We can't rely on the callback for suspension because it is sometimes not called by iOS
        // (for instance when the web socket is closing at the same time).
        // To avoid suspending forever in those cases, we just never suspend.
        webSocket.sendMessage(message) { err ->
            if (err != null) {
                println("Error while sending websocket message: $err")
            }
        }
    }

    override suspend fun sendPing(frameData: ByteArray) {
        webSocket.sendPingWithPongReceiveHandler { err ->
            if (err != null) {
                println("Error while sending websocket ping: $err")
            }
        }
    }

    override suspend fun close(code: Int, reason: String?) {
        webSocket.cancelWithCloseCode(code.convert(), reason?.encodeToNSData())
    }
}

/**
 * Listens to the incoming messages on this web socket and forwards them to the given [incomingFrames] channel.
 *
 * This method is implemented with recursion due to the peculiar design of NSURLSessionWebSocketTask.
 * There is currently no way to register a callback for all new messages - only to listen to one single "next" message.
 */
private fun NSURLSessionWebSocketTask.forwardNextIncomingMessagesAsyncTo(incomingFrames: SendChannel<WebSocketFrame>) {
    receiveMessageWithCompletionHandler { message, nsError ->
        when {
            nsError != null -> {
                // We sometimes get this error: Domain=NSPOSIXErrorDomain Code=57 "Socket is not connected"
                // It happens when the websocket is closed normally, in which case we just don't fail here, and the
                // channel will be closed in the actual NSURLSession close callback.
                if (nsError.code.toInt() == ERROR_CODE_SOCKET_NOT_CONNECTED) {
                    // If the channel is not closed, it might be worth continuing to forward messages (maybe we got this
                    // error for another reason, like the websocket was not connected *yet*) - it shouldn't hurt anyway.
                    // TODO check if we actually do get this error in this case, and thus continuing to forward new
                    //  messages is indeed useful
                    if (!incomingFrames.isClosedForSend) {
                        forwardNextIncomingMessagesAsyncTo(incomingFrames)
                    }
                    return@receiveMessageWithCompletionHandler
                }
                incomingFrames.close(nsError.toIosWebSocketException())
                // No recursive call here, so we stop listening to messages in a closed or failed web socket
            }
            message != null -> {
                val result = incomingFrames.trySend(message.toWebSocketFrame())
                if (result.isFailure) {
                    val closeException = WebSocketException("Could not pass message frame through channel", cause = result.exceptionOrNull())
                    incomingFrames.close(closeException)
                    // still throw because no one might be listening to this channel (especially since the buffer is likely full)
                    throw closeException
                }
                if (result.isClosed) {
                    // TODO should we throw here instead? In which cases exactly this can happen?
                    // if the channel is already closed, maybe it is just a race with the closing handshake
                    // and we can simply ignore the extra message
                    return@receiveMessageWithCompletionHandler
                }
                // it's ok to use recursion since the call is asynchronous anyway, we won't blow the stack
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
    NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.convert())
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

private fun NSError.toIosWebSocketException(): WebSocketException = DarwinWebSocketException(this)

private fun NSError.toWebSocketHandshakeException(
    urlResponse: NSURLResponse? = null
): WebSocketException {
    val httpResponse = urlResponse as? NSHTTPURLResponse
    val statusCode = httpResponse?.statusCode?.toInt()
    return DarwinWebSocketHandshakeException(this, statusCode)
}

/**
 * A [WebSocketException] caused by a darwin [NSError].
 * It contains details about the actual error cause.
 */
class DarwinWebSocketException(
    val nsError: NSError,
) : WebSocketException(nsError.description ?: nsError.localizedDescription)

/**
 * A [WebSocketException] caused by a darwin [NSError], during the handshake phase.
 * It contains details about the actual error cause, as well as the [httpStatusCode], if applicable.
 */
class DarwinWebSocketHandshakeException(
    val nsError: NSError,
    val httpStatusCode: Int?,
) : WebSocketException(handshakeExceptionMessage(nsError, httpStatusCode))

private fun handshakeExceptionMessage(nsError: NSError, httpStatusCode: Int?): String {
    // [baseMessage] will look something like:
    // Error Domain=<domain> Code=<code> UserInfo={NSLocalizedDescription=<localized_description>}
    val baseMessage = nsError.description ?: nsError.localizedDescription
    return if (httpStatusCode != null) {
        "$baseMessage HTTP Status Code=$httpStatusCode"
    } else {
        baseMessage
    }
}
