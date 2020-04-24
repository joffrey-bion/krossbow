package org.hildan.krossbow.stomp

import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.frame.asText
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.utils.generateUuid

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
     * Depending on the configuration, headers may be modified before sending the frame:
     *
     * - If no `receipt` header is provided and [auto-receipt][StompConfig.autoReceipt] is enabled, a new unique receipt
     * header is generated and added
     * - If no `content-length` header is provided and [autoContentLength][StompConfig.autoContentLength] is enabled, the
     * content length is computed based on body size and added as `content-length` header
     *
     * @return null right after sending the frame if auto-receipt is disabled and no receipt header is provided.
     * Otherwise this method suspends until the relevant RECEIPT frame is received from the server, and then returns
     * a [StompReceipt].
     * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
     * a [LostReceiptException] is thrown.
     */
    suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt?

    /**
     * Sends a SUBSCRIBE frame with the given [headers], converting received messages into objects of type [T] using
     * [convertMessage].
     * The returned [StompSubscription] can be used to access the channel of received objects and unsubscribe.
     *
     * If no `receipt` header is provided and [auto-receipt][StompConfig.autoReceipt] is enabled, a new unique receipt
     * header is generated and added.
     *
     * If a receipt header is present (automatically added or manually provided), this method suspends until the
     * relevant RECEIPT frame is received from the server.
     * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
     * a [LostReceiptException] is thrown.
     *
     * If auto-receipt is disabled and no `receipt` header is provided, this method returns immediately.
     */
    suspend fun <T> subscribe(
        headers: StompSubscribeHeaders,
        convertMessage: (StompFrame.Message) -> T
    ): StompSubscription<T>

    /**
     * Sends an ACK frame with the given [ackId].
     *
     * The provided [ackId] must match the `ack` header of the message to acknowledge.
     * If this acknowledgement is part of a transaction, the [transactionId] should be provided.
     */
    suspend fun ack(ackId: String, transactionId: String? = null)

    /**
     * Sends a NACK frame with the given [ackId].
     *
     * The provided [ackId] must match the `ack` header of the message to refuse.
     * If this acknowledgement is part of a transaction, the [transactionId] should be provided.
     */
    suspend fun nack(ackId: String, transactionId: String? = null)

    /**
     * Sends a BEGIN frame with the given [transactionId].
     */
    suspend fun begin(transactionId: String)

    /**
     * Sends a COMMIT frame with the given [transactionId].
     */
    suspend fun commit(transactionId: String)

    /**
     * Sends an ABORT frame with the given [transactionId].
     */
    suspend fun abort(transactionId: String)

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
 * Subscribes to the given [destination], converting received messages into objects of type [T] using
 * [convertMessage].
 * The returned [StompSubscription] can be used to access the channel of received objects and unsubscribe.
 *
 * If auto-receipt is enabled, this method suspends until the relevant RECEIPT frame is received from the server.
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 *
 * If auto-receipt is disabled, this method returns immediately.
 */
suspend fun <T> StompSession.subscribe(
    destination: String,
    convertMessage: (StompFrame.Message) -> T
): StompSubscription<T> = subscribe(StompSubscribeHeaders(destination), convertMessage)

/**
 * Subscribes to the given [destination], expecting raw message frames.
 * The returned [StompSubscription] can be used to access the channel of received messages and unsubscribe.
 *
 * If auto-receipt is enabled, this method suspends until the relevant RECEIPT frame is received from the server.
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 *
 * If auto-receipt is disabled, this method returns immediately.
 */
suspend fun StompSession.subscribeRaw(destination: String): StompSubscription<StompFrame.Message> =
    subscribe(destination) { it }

/**
 * Subscribes to the given [destination], expecting text message bodies.
 * The returned [StompSubscription] can be used to access the channel of received messages and unsubscribe.
 * Frames without a body are seen as null values in the messages channel of the subscription.
 *
 * If auto-receipt is enabled, this method suspends until the relevant RECEIPT frame is received from the server.
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 *
 * If auto-receipt is disabled, this method returns immediately.
 */
suspend fun StompSession.subscribeText(destination: String): StompSubscription<String?> =
    subscribe(destination) { it.body?.asText() }

/**
 * Subscribes to the given [destination], expecting binary message bodies.
 * The returned [StompSubscription] can be used to access the channel of received messages and unsubscribe.
 * Frames without a body are seen as null values in the messages channel of the subscription.
 *
 * If auto-receipt is enabled, this method suspends until the relevant RECEIPT frame is received from the server.
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 *
 * If auto-receipt is disabled, this method returns immediately.
 */
suspend fun StompSession.subscribeBinary(destination: String): StompSubscription<ByteArray?> =
    subscribe(destination) { it.body?.bytes }

/**
 * Subscribes to the given [destination], ignoring the body of the received messages.
 *
 * If auto-receipt is enabled, this method suspends until the relevant RECEIPT frame is received from the server.
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 *
 * If auto-receipt is disabled, this method returns immediately.
 */
suspend fun StompSession.subscribeEmptyMsg(destination: String): StompSubscription<Unit> =
    subscribe(destination) { Unit }

/**
 * Executes the given [block] as part of a transaction.
 *
 * This method automatically generates an ID for the new transaction and sends a BEGIN frame.
 * The given [block] is given the generated transaction ID as parameter.
 * The receiver of the given [block] is a special [StompSession] that automatically fills the `transaction` header
 * (if absent) for all SEND, ACK, and NACK frames.
 *
 * The transaction is committed if the block executes successfully, and aborted in case of exception.
 * Any exception thrown by the block is re-thrown after sending the ABORT frame.
 */
suspend fun <T> StompSession.withTransaction(block: StompSession.(transactionId: String) -> T): T {
    val transactionId = generateUuid()
    begin(transactionId)
    try {
        val result = TransactionStompSession(this, transactionId).block(transactionId)
        commit(transactionId)
        return result
    } catch (e: Exception) {
        abort(transactionId)
        throw e
    }
}

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
