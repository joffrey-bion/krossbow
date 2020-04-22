package org.hildan.krossbow.stomp

import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.frame.asText
import org.hildan.krossbow.stomp.headers.AckMode
import org.hildan.krossbow.stomp.headers.StompAckHeaders
import org.hildan.krossbow.stomp.headers.StompNackHeaders
import org.hildan.krossbow.stomp.headers.StompSendHeaders

/**
 * A coroutine-based STOMP session. This interface defines interactions with a STOMP server.
 *
 * ### Subscriptions
 *
 * Subscriptions are channel-based.
 * This is not to avoid callbacks at all costs, but rather to separate internal coroutines processing from the user
 * code processing.
 * Error handling is done through the subscription channels: if something fails, channel consumers will crash with
 * the relevant exception.
 *
 * Various extension functions are available to subscribe to a destination with different message conversions.
 *
 * ### Heart beats
 *
 * When configured, heart beats can be used as a keep-alive to detect if the connection is lost.
 * The [StompConfig.heartBeat] property should be used to configure heart beats in the [StompClient].
 *
 * Sending heart beats is automatically handled by StompSession implementations.
 * If expected heart beats are not received in time, a [MissingHeartBeatException] is thrown and fails active
 * subscriptions.
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
 * in the configured [time limit][StompConfig.receiptTimeoutMillis], a [LostReceiptException] is thrown.
 *
 * If no receipt is provided and auto-receipt is disabled, the [send] and [subscribe] methods return immediately
 * after the underlying web socket implementation has returned from sending the frame.
 */
interface StompSession {

    /**
     * Sends a SEND frame to the server with the given [headers] and the given [body].
     *
     * @return null right after sending the frame if auto-receipt is disabled and no receipt header is provided.
     * Otherwise this method suspends until the relevant RECEIPT frame is received from the server, and then returns
     * a [StompReceipt].
     * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
     * a [LostReceiptException] is thrown.
     */
    suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt?

    /**
     * Subscribes to the given [destination], converting received messages into objects of type [T] using
     * [convertMessage].
     * The returned [StompSubscription] can be used to access the channel of received objects and unsubscribe.
     *
     * If auto-receipt is enabled or if a non-null [receiptId] is provided, this method suspends until the relevant
     * RECEIPT frame is received from the server.
     * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
     * a [LostReceiptException] is thrown.
     *
     * If auto-receipt is disabled and no [receiptId] is provided, this method returns immediately.
     */
    suspend fun <T> subscribe(
        destination: String,
        receiptId: String? = null,
        ackMode: AckMode? = null,
        convertMessage: (StompFrame.Message) -> T
    ): StompSubscription<T>

    /**
     * Sends an ACK frame with the given [headers].
     */
    suspend fun ack(headers: StompAckHeaders)

    /**
     * Sends a NACK frame with the given [headers].
     */
    suspend fun nack(headers: StompNackHeaders)

    /**
     * If [graceful disconnect][StompConfig.gracefulDisconnect] is enabled (which is the default), sends a DISCONNECT
     * frame to close the session, waits for the relevant RECEIPT frame, and then closes the connection. Otherwise,
     * force-closes the connection.
     *
     * If a RECEIPT frame is not received within the [configured time][StompConfig.disconnectTimeoutMillis], this
     * function doesn't throw an exception, it returns normally.
     */
    suspend fun disconnect()
}

/**
 * A STOMP receipt, as
 * [defined in the STOMP specification](https://stomp.github.io/stomp-specification-1.2.html#RECEIPT).
 */
data class StompReceipt(
    /**
     * The value of the receipt header sent to the server, and returned in a RECEIPT frame.
     */
    val id: String
)

/**
 * Sends a SEND frame to the server at the given [destination] with the given binary [body].
 *
 * @return null right after sending the frame if auto-receipt is disabled.
 * Otherwise this method suspends until the relevant RECEIPT frame is received from the server, and then returns
 * a [StompReceipt].
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 */
suspend fun StompSession.sendBinary(destination: String, body: ByteArray?): StompReceipt? =
        send(StompSendHeaders(destination), body?.let { FrameBody.Binary(it) })

/**
 * Sends a SEND frame to the server at the given [destination] with the given textual [body].
 *
 * @return null right after sending the frame if auto-receipt is disabled.
 * Otherwise this method suspends until the relevant RECEIPT frame is received from the server, and then returns
 * a [StompReceipt].
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 */
suspend fun StompSession.sendText(destination: String, body: String?): StompReceipt? =
        send(StompSendHeaders(destination), body?.let { FrameBody.Text(it) })

/**
 * Sends a SEND frame to the server at the given [destination] without body.
 *
 * @return null right after sending the frame if auto-receipt is disabled.
 * Otherwise this method suspends until the relevant RECEIPT frame is received from the server, and then returns
 * a [StompReceipt].
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 */
suspend fun StompSession.sendEmptyMsg(destination: String): StompReceipt? = send(StompSendHeaders(destination), null)

/**
 * Subscribes to the given [destination], expecting raw message frames.
 * The returned [StompSubscription] can be used to access the channel of received messages and unsubscribe.
 *
 * If auto-receipt is enabled or if a non-null [receiptId] is provided, this method suspends until the relevant
 * RECEIPT frame is received from the server.
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 *
 * If auto-receipt is disabled and no [receiptId] is provided, this method returns immediately.
 */
suspend fun StompSession.subscribeRaw(
    destination: String,
    receiptId: String? = null,
    ackMode: AckMode? = null
): StompSubscription<StompFrame.Message> = subscribe(destination, receiptId, ackMode) { it }

/**
 * Subscribes to the given [destination], expecting text message bodies.
 * The returned [StompSubscription] can be used to access the channel of received messages and unsubscribe.
 * Frames without a body are seen as a null value in the messages channel of the subscription.
 *
 * If auto-receipt is enabled or if a non-null [receiptId] is provided, this method suspends until the relevant
 * RECEIPT frame is received from the server.
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 *
 * If auto-receipt is disabled and no [receiptId] is provided, this method returns immediately.
 */
suspend fun StompSession.subscribeText(
    destination: String,
    receiptId: String? = null,
    ackMode: AckMode? = null
): StompSubscription<String?> = subscribe(destination, receiptId, ackMode) { it.body?.asText() }

/**
 * Subscribes to the given [destination], expecting binary message bodies.
 * The returned [StompSubscription] can be used to access the channel of received messages and unsubscribe.
 * Frames without a body are seen as a null value in the messages channel of the subscription.
 *
 * If auto-receipt is enabled or if a non-null [receiptId] is provided, this method suspends until the relevant
 * RECEIPT frame is received from the server.
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 *
 * If auto-receipt is disabled and no [receiptId] is provided, this method returns immediately.
 */
suspend fun StompSession.subscribeBinary(
    destination: String,
    receiptId: String? = null,
    ackMode: AckMode? = null
): StompSubscription<ByteArray?> = subscribe(destination, receiptId, ackMode) { it.body?.bytes }

/**
 * Subscribes to the given [destination], ignoring the body of the received messages.
 *
 * If auto-receipt is enabled or if a non-null [receiptId] is provided, this method suspends until the relevant
 * RECEIPT frame is received from the server.
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 *
 * If auto-receipt is disabled and no [receiptId] is provided, this method returns immediately.
 */
suspend fun StompSession.subscribeEmptyMsg(
    destination: String,
    receiptId: String? = null,
    ackMode: AckMode? = null
): StompSubscription<Unit> = subscribe(destination, receiptId, ackMode) { Unit }

/**
 * Sends an ACK frame with the given ack [id].
 *
 * The provided [id] must match the `ack` header of the message to acknowledge.
 * If this acknowledgement is part of the transaction, the [transaction] id should be provided.
 */
suspend fun StompSession.ack(id: String, transaction: String? = null) = ack(StompAckHeaders(id, transaction))

/**
 * Sends a NACK frame with the given ack [id].
 *
 * The provided [id] must match the `ack` header of the message to refuse.
 * If this acknowledgement is part of the transaction, the [transaction] id should be provided.
 */
suspend fun StompSession.nack(id: String, transaction: String? = null) = nack(StompNackHeaders(id, transaction))

/**
 * Executes the given block on this [StompSession], and [disconnects][StompSession.disconnect] from the session whether
 * the block terminated normally or exceptionally.
 */
suspend fun <S : StompSession, T> S.use(block: suspend S.() -> T): T {
    try {
        return block()
    } finally {
        disconnect()
    }
}
