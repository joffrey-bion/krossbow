package org.hildan.krossbow.stomp

import kotlinx.coroutines.channels.ReceiveChannel
import org.hildan.krossbow.converters.MessageConverter
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import kotlin.reflect.KClass

/**
 * A coroutine-based STOMP session.
 *
 * ### Message conversion
 *
 * The [send] and [subscribe] methods have some overloads allowing arbitrary payload types to be sent/received. When
 * using these overloads, the configured [message converter][StompConfig.messageConverter] is used to convert between
 * objects and frame payloads.
 *
 * ### Suspension & Receipts
 *
 * The STOMP protocol supports RECEIPT frames, allowing the client to know when the server has received a frame.
 * This only happens if a receipt header is set on the client frame.
 * If [auto-receipt][StompConfig.autoReceipt] is enabled, all client frames supporting the mechanism will be
 * automatically given such a header. If auto-receipt is not enabled, a receipt header may be provided manually.
 *
 * When a receipt header is present (automatically added or manually provided), all [send] and [subscribe] overloads
 * suspend until the relevant RECEIPT frame is received from the server. If no RECEIPT frame is received from the server
 * in the configured [time limit][StompConfig.receiptTimeLimit], a [LostReceiptException] is thrown.
 *
 * If no receipt is provided and auto-receipt is disabled, the [send] and [subscribe] methods return immediately
 * after the underlying web socket implementation has returned.
 */
interface StompSession {

    /**
     * Sends a SEND frame to the server with the given [headers] and the given [body].
     *
     * @return null (immediately) if auto-receipt is disabled and no receipt header is provided. Otherwise this method
     * suspends until the relevant RECEIPT frame is received from the server, and then returns a [StompReceipt].
     */
    suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt?

    /**
     * Sends a SEND frame to the server with the given [headers] and the given [payload]. The payload will be converted
     * via the configured [MessageConverter].
     *
     * @return null (immediately) if auto-receipt is disabled and no receipt header is provided. Otherwise this method
     * suspends until the relevant RECEIPT frame is received from the server, and then returns a [StompReceipt].
     */
    suspend fun <T : Any> send(headers: StompSendHeaders, payload: T? = null, payloadType: KClass<T>): StompReceipt?

    /**
     * Subscribes to the given [destination], expecting objects of type [T]. The returned [StompSubscription]
     * can be used to access the channel of received objects and unsubscribe.
     *
     * The configured [MessageConverter] is used to create instances of the given type from the body of every message
     * received on the created subscription. If no payload is received in a message, it's up to the implementation of
     * the [MessageConverter] to decide what to do. If you want to bypass the type converter completely for this
     * subscription, use [subscribeNoPayload] instead.
     *
     * If auto-receipt is enabled or if a non-null [receiptId] is provided, this method suspends until the relevant
     * RECEIPT frame is received from the server. If no RECEIPT frame is received from the server
     * in the configured [time limit][StompConfig.receiptTimeLimit], a [LostReceiptException] is thrown.
     *
     * If auto-receipt is disabled and no [receiptId] is provided, this method returns immediately.
     */
    suspend fun <T : Any> subscribe(
        destination: String,
        clazz: KClass<T>,
        receiptId: String? = null
    ): StompSubscription<T>

    /**
     * Subscribes to the given [destination], ignoring message payloads.
     *
     * If auto-receipt is enabled or if a non-null [receiptId] is provided, this method suspends until the relevant
     * RECEIPT frame is received from the server. If no RECEIPT frame is received from the server
     * in the configured [time limit][StompConfig.receiptTimeLimit], a [LostReceiptException] is thrown.
     *
     * If auto-receipt is disabled and no [receiptId] is provided, this method returns immediately.
     */
    suspend fun subscribeNoPayload(destination: String, receiptId: String? = null): StompSubscription<Unit>

    /**
     * If [graceful disconnect][StompConfig.gracefulDisconnect] is enabled (which is the default), sends a DISCONNECT
     * frame to close the session, waits for the relevant RECEIPT frame, and then closes the connection. Otherwise,
     * force-closes the connection.
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
    /**
     * The subscription messages channel, to read incoming messages from.
     */
    val messages: ReceiveChannel<StompMessage<T>>
    /**
     * Unsubscribes from this subscription to stop receive messages. This closes the [messages] channel, so that any
     * loop on it stops as well.
     */
    suspend fun unsubscribe()
}

/**
 * Sends a SEND frame to the server at the given [destination] with the given binary [body].
 *
 * Please refer to [StompSession.send] for details about how receipts are handled.
 */
suspend fun StompSession.sendBinary(destination: String, body: ByteArray?): StompReceipt? =
        send(StompSendHeaders(destination), body?.let { FrameBody.Binary(it) })

/**
 * Sends a SEND frame to the server at the given [destination] with the given textual [body].
 *
 * Please refer to [StompSession.send] for details about how receipts are handled.
 */
suspend fun StompSession.sendText(destination: String, body: String?): StompReceipt? =
        send(StompSendHeaders(destination), body?.let { FrameBody.Text(it) })

/**
 * Sends a SEND frame to the server at the given [destination] with no payload.
 *
 * Please refer to [StompSession.send] for details about how receipts are handled.
 */
suspend fun StompSession.send(destination: String): StompReceipt? = send(StompSendHeaders(destination), null)

/**
 * Sends a SEND frame to the server at the given [destination] with the given [payload].
 *
 * Please refer to [StompSession.send] for details about how receipts are handled.
 */
suspend fun <T : Any> StompSession.send(destination: String, payload: T? = null, payloadType: KClass<T>): StompReceipt? =
    send(StompSendHeaders(destination), payload, payloadType)

/**
 * Sends a SEND frame to the server at the given [destination] with the given [payload].
 *
 * Please refer to [StompSession.send] for details about how receipts are handled.
 */
suspend inline fun <reified T : Any> StompSession.send(destination: String, payload: T?): StompReceipt? =
        send(destination, payload, T::class)

/**
 * Subscribes to the given [destination], expecting objects of type [T]. The returned [StompSubscription]
 * can be used to access the channel of received objects.
 *
 * A platform-specific deserializer is used to create instances of the given type from the body of every message
 * received on the created subscription.
 */
suspend inline fun <reified T : Any> StompSession.subscribe(
    destination: String,
    receiptId: String? = null
): StompSubscription<T> = subscribe(destination, T::class, receiptId)

/**
 * Exception thrown when the websocket connection + STOMP connection takes too much time.
 */
class ConnectionTimeout(message: String, cause: Exception) : Exception(message, cause)

/**
 * Exception thrown when a STOMP error frame is received.
 */
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
