package org.hildan.krossbow.stomp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.utils.generateUuid

/**
 * A coroutine-based STOMP session. This interface defines interactions with a STOMP server.
 *
 * ### Subscriptions
 *
 * Subscriptions are [Flow]-based. All [subscribe] overloads immediately return a cold [Flow] (and don't suspend).
 * No subscription happens when calling these methods.
 *
 * The actual subscription (with a SUBSCRIBE frame) only occurs when a terminal operator is used on the flow.
 *
 * If no specific subscription ID is provided in the SUBSCRIBE headers, the returned flow can safely be collected
 * multiple times, even concurrently.
 * This will simply result in independent subscriptions with different IDs.
 *
 * If an ID is manually provided in the headers, the flow must only be collected once.
 * Multiple collections of the flow result in an unspecified behaviour.
 *
 * Subscription cancellation via UNSUBSCRIBE frame is automatically handled in the following situations:
 * - the flow consumer's job is cancelled
 * - the flow consumer throws an exception
 * - the flow consumer uses a terminal operator that ends the flow early, such as [first][kotlinx.coroutines.flow.first]
 *
 * If an error occurs upstream (e.g. STOMP ERROR frame or unexpected web socket closure), then all subscription flow
 * collectors throw the relevant exception.
 *
 * Various extension functions are available to subscribe to a destination with predefined message conversions.
 * You can also apply your own operators on the returned flows to convert/handle message frames.
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
 * When a receipt header is present (automatically added or manually provided), the method that is used to send the
 * frame suspends until the relevant RECEIPT frame is received from the server.
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown.
 *
 * If no receipt is provided and auto-receipt is disabled, the method used to send the frame returns immediately
 * after the underlying web socket implementation has returned from sending the frame.
 *
 * This suspend-until-receipt behaviour is less noticeable for subscription methods because they return cold flows.
 * The suspension doesn't occur when calling the [subscribe] method itself, but when collecting the flow.
 * This means that, if a receipt header is present, the [collect][Flow.collect] call will expect a RECEIPT frame from
 * the server corresponding to the SUBSCRIBE frame, and won't start receiving messages until then.
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
 * a [LostReceiptException] is thrown in the collector's code.
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
     * Returns a cold [Flow] of [MESSAGE][StompFrame.Message] frames that subscribes on [collect][Flow.collect], and
     * unsubscribes when the consuming coroutine is cancelled.
     *
     * The subscription is done by sending a SUBSCRIBE frame with the given [headers].
     * The unsubscription is done by sending an UNSUBSCRIBE frame.
     *
     * If no `receipt` header is provided and [auto-receipt][StompConfig.autoReceipt] is enabled, a new unique receipt
     * header is generated and added.
     *
     * If a receipt header is present (automatically added or manually provided), the [collect][Flow.collect] call will
     * expect a RECEIPT frame from the server corresponding to the SUBSCRIBE frame.
     * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeoutMillis],
     * a [LostReceiptException] is thrown in the collector's code.
     *
     * If auto-receipt is disabled and no `receipt` header is provided, the `collect` call doesn't wait for a RECEIPT
     * frame and never throws [LostReceiptException].
     */
    fun subscribe(headers: StompSubscribeHeaders): Flow<StompFrame.Message>

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
 * Returns a cold [Flow] of [MESSAGE][StompFrame.Message] frames that subscribes on [collect][Flow.collect], and
 * unsubscribes when the consumer is cancelled.
 *
 * See the general [StompSession] documentation for more details about subscription flows and receipts.
 */
fun StompSession.subscribe(destination: String): Flow<StompFrame.Message> =
    subscribe(StompSubscribeHeaders(destination))

/**
 * Returns a cold [Flow] of messages that subscribes on [collect][Flow.collect], and unsubscribes when the consuming
 * coroutine is cancelled.
 *
 * The received MESSAGE frames' bodies are expected to be decodable as text (their `content-type` header should start
 * with `text/` or contain a `charset` parameter).
 * If a received frame is not decodable as text, an exception is thrown.
 * Frames without a body are indistinguishable from frames with a 0-length body, and therefore result in an empty
 * string in the subscription flow.
 *
 * See the general [StompSession] documentation for more details about subscription flows and receipts.
 */
fun StompSession.subscribeText(destination: String): Flow<String> =
    subscribe(destination).map { it.bodyAsText }

/**
 * Returns a cold [Flow] of binary message bodies, that subscribes on [collect][Flow.collect] and unsubscribes when
 * the consuming coroutine is cancelled.
 * Frames without a body are indistinguishable from frames with a 0-length body, and therefore result in an empty
 * [ByteArray] in the subscription flow.
 *
 * See the general [StompSession] documentation for more details about subscription flows and receipts.
 */
fun StompSession.subscribeBinary(destination: String): Flow<ByteArray> =
    subscribe(destination).map { it.body?.bytes ?: ByteArray(0) }

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
