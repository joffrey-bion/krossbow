package org.hildan.krossbow.stomp.session

import kotlinx.coroutines.channels.ReceiveChannel
import org.hildan.krossbow.converters.MessageConverter
import org.hildan.krossbow.stomp.KrossbowMessage
import org.hildan.krossbow.stomp.KrossbowReceipt
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import kotlin.reflect.KClass

interface StompSession {

    /**
     * Sends a SEND frame to the server with the given [headers] and the given [body].
     *
     * If auto-receipt is enabled or if a `receipt` header is provided, this method suspends until a RECEIPT
     * frame is received from the server and returns a [KrossbowReceipt]. If no RECEIPT frame is received from the
     * server in the configured time limit, a [LostReceiptException] is thrown.
     *
     * If receipts are not enabled, this method sends the frame and immediately returns null.
     */
    suspend fun send(headers: StompSendHeaders, body: FrameBody?): KrossbowReceipt?

    /**
     * Sends a SEND frame to the server with the given [headers] and the given [payload]. The payload will be converted
     * via the configured [MessageConverter].
     *
     * If auto-receipt is enabled or if a `receipt` header is provided, this method suspends until a RECEIPT
     * frame is received from the server and returns a [KrossbowReceipt]. If no RECEIPT frame is received from the
     * server in the configured time limit, a [LostReceiptException] is thrown.
     *
     * If receipts are not enabled, this method sends the frame and immediately returns null.
     */
    suspend fun <T : Any> send(headers: StompSendHeaders, payload: T? = null, payloadType: KClass<T>): KrossbowReceipt?

    /**
     * Subscribes to the given [destination], expecting objects of type [T]. The returned [StompSubscription]
     * can be used to access the channel of received objects and unsubscribe.
     *
     * The configured [MessageConverter] is used to create instances of the given type from the body of every message
     * received on the created subscription. If no payload is received in a message, it's up to the implementation of
     * the [MessageConverter] to decide what to do. If you want to bypass the type converter completely for this
     * subscription, use [subscribeNoPayload] instead.
     */
    suspend fun <T : Any> subscribe(destination: String, clazz: KClass<T>): StompSubscription<T>

    /**
     * Subscribes to the given [destination], ignoring message payloads.
     */
    suspend fun subscribeNoPayload(destination: String): StompSubscription<Unit>

    /**
     * Sends a DISCONNECT frame to close the session, and closes the connection.
     */
    suspend fun disconnect()
}

/**
 * A subscription to a STOMP destination, streaming messages of type [T].
 */
interface StompSubscription<out T> {
    /**
     * The subscription ID used by the STOMP protocol.
     */
    val id: String
    /** The subscription messages channel, to read incoming messages from. */
    val messages: ReceiveChannel<KrossbowMessage<T>>
    /**
     * Unsubscribes from this subscription to stop receive messages. This closes the [messages] channel, so that any
     * loop on it stops as well.
     */
    suspend fun unsubscribe()
}

/**
 * Sends a SEND frame to the server at the given [destination] with no payload.
 *
 * Please refer to [StompSession.send] for details about how receipts are handled.
 */
suspend fun StompSession.send(destination: String): KrossbowReceipt? = send(StompSendHeaders(destination), null)

/**
 * Sends a SEND frame to the server at the given [destination] with the given binary [body].
 *
 * Please refer to [StompSession.send] for details about how receipts are handled.
 */
suspend fun StompSession.sendBinary(destination: String, body: ByteArray?): KrossbowReceipt? =
        send(StompSendHeaders(destination), body?.let { FrameBody.Binary(it) })

/**
 * Sends a SEND frame to the server at the given [destination] with the given textual [body].
 *
 * Please refer to [StompSession.send] for details about how receipts are handled.
 */
suspend fun StompSession.sendText(destination: String, body: String?): KrossbowReceipt? =
        send(StompSendHeaders(destination), body?.let { FrameBody.Text(it) })

/**
 * Sends a SEND frame to the server at the given [destination] with the given [payload].
 *
 * Please refer to [StompSession.send] for details about how receipts are handled.
 */
suspend fun <T : Any> StompSession.send(destination: String, payload: T? = null, payloadType: KClass<T>): KrossbowReceipt? =
    send(StompSendHeaders(destination), payload, payloadType)

/**
 * Sends a SEND frame to the server at the given [destination] with the given [payload].
 *
 * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received from the server and returns a
 * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
 * [LostReceiptException] is thrown.
 *
 * If receipts are not enabled, this method sends the frame and immediately returns null.
 */
suspend inline fun <reified T : Any> StompSession.send(destination: String, payload: T?): KrossbowReceipt? =
        send(destination, payload, T::class)

/**
 * Subscribes to the given [destination], expecting objects of type [T]. The returned [StompSubscription]
 * can be used to access the channel of received objects.
 *
 * A platform-specific deserializer is used to create instances of the given type from the body of every message
 * received on the created subscription.
 */
suspend inline fun <reified T : Any> StompSession.subscribe(destination: String): StompSubscription<T> =
        subscribe(destination, T::class)


class StompErrorFrameReceived(val frame: StompFrame.Error) : Exception("STOMP ERROR frame received: ${frame.message}")

/**
 * An exception thrown when a RECEIPT frame was expected from the server, but not received in the configured time limit.
 */
class LostReceiptException(
    /**
     * The value of the receipt header sent to the server, and expected in a RECEIPT frame.
     */
    val receiptId: String
) : Exception("No RECEIPT frame received for receiptId '$receiptId' within the configured time limit")
