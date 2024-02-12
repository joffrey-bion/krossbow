package org.hildan.krossbow.stomp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.io.bytestring.*
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.stomp.utils.generateUuid

/**
 * A coroutine-based STOMP session. This interface defines interactions with a STOMP server.
 *
 * ### Suspension & Receipts
 *
 * The STOMP protocol supports RECEIPT frames, allowing the client to know when the server has received a frame.
 * This only happens if a receipt header is set on the client frame.
 *
 * If [auto-receipt][StompConfig.autoReceipt] is enabled, a `receipt` header is automatically generated and added to
 * all client frames supporting the mechanism, and for which a `receipt` header is not already present.
 * If auto-receipt is not enabled, a receipt header may still be provided manually in the parameters of some overloads.
 *
 * When a receipt header is present (automatically added or manually provided), the method that is used to send the
 * frame suspends until the corresponding RECEIPT frame is received from the server.
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeout],
 * a [LostReceiptException] is thrown.
 *
 * If no receipt is provided and auto-receipt is disabled, the method used to send the frame doesn't wait for a
 * RECEIPT frame and never throws [LostReceiptException].
 * Instead, it returns immediately after the underlying web socket implementation is done sending the frame.
 *
 * ### Subscriptions
 *
 * The [subscribe] overloads are a bit unconventional because they are suspending and yet they return a [Flow].
 *
 * They make use of the suspension mechanism to allow callers to wait for the subscription to actually happen.
 * This can only be guaranteed when making use of the receipts mechanism (please see the previous section).
 * All [subscribe] overloads send a SUBSCRIBE frame immediately, and suspend until the corresponding RECEIPT frame is
 * received (when applicable) or when the SUBSCRIBE frame is sent (when the receipt mechanism is not used).
 *
 * The returned [Flow] is the flow of messages that are received as part of the subscription.
 * This flow automatically unsubscribes by sending an UNSUBSCRIBE frame in the following situations:
 *
 * - the flow collector's job is cancelled
 * - the flow collector's code throws an exception
 * - the flow's consumer uses a terminal operator that ends the flow early, such as [first][kotlinx.coroutines.flow.first]
 *
 * Because of this automatic unsubscription, the returned flow can only be collected once.
 * Multiple collections of the returned flow (parallel or sequential) result in an unspecified behaviour.
 *
 * If an error occurs upstream (e.g. STOMP ERROR frame or unexpected web socket closure), then all subscription flow
 * collectors throw the relevant exception, but no UNSUBSCRIBE frame is sent (because the connection is failed).
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
 */
interface StompSession {

    /**
     * Sends a SEND frame to the server with the given [headers] and the given [body].
     *
     * Depending on the configuration, headers may be modified before sending the frame:
     *
     * - If no `receipt` header is provided and [auto-receipt][StompConfig.autoReceipt] is enabled, a new unique receipt
     * header is generated and added
     * - If no `content-length` header is provided and [autoContentLength][StompConfig.autoContentLength] is enabled,
     * the content length is computed based on body size and added as `content-length` header
     *
     * If a `receipt` header is present (automatically added or manually provided), this method suspends until the
     * corresponding RECEIPT frame is received from the server.
     * If no RECEIPT frame is received in the configured [time limit][StompConfig.receiptTimeout], a
     * [LostReceiptException] is thrown.
     *
     * If auto-receipt is disabled and no `receipt` header is provided, this method doesn't wait for a RECEIPT frame
     * and never throws [LostReceiptException].
     * Instead, it returns immediately after sending the SEND frame.
     *
     * See the general [StompSession] documentation for more details about suspension and receipts.
     *
     * @return the [StompReceipt] received from the server if receipts are enabled, or null if receipts were not used
     *
     * @see sendBinary
     * @see sendText
     * @see sendEmptyMsg
     */
    // No default value null on purpose, because sendEmptyMsg() should be used if we statically know there is no body
    suspend fun send(headers: StompSendHeaders, body: FrameBody?): StompReceipt?

    /**
     * Subscribes and returns a [Flow] of [MESSAGE][StompFrame.Message] frames that unsubscribes automatically when the
     * collector is done or cancelled.
     * The returned flow can be collected only once.
     *
     * The subscription happens immediately by sending a [SUBSCRIBE][StompFrame.Subscribe] frame with the provided
     * headers.
     * If no subscription ID is provided in the [headers], a generated ID is used in the SUBSCRIBE frame.
     *
     * If no `receipt` header is provided and [auto-receipt][StompConfig.autoReceipt] is enabled, a new unique `receipt`
     * header is generated and added to the SUBSCRIBE frame.
     *
     * If a `receipt` header is present (automatically added or manually provided), this method suspends until the
     * corresponding RECEIPT frame is received from the server.
     * If no RECEIPT frame is received in the configured [time limit][StompConfig.receiptTimeout], a
     * [LostReceiptException] is thrown.
     *
     * If auto-receipt is disabled and no `receipt` header is provided, this method doesn't wait for a RECEIPT frame
     * and never throws [LostReceiptException].
     * Instead, it returns immediately after sending the SUBSCRIBE frame.
     * This means that, in this case, there is no real guarantee that the subscription actually happened when this
     * method returns.
     *
     * The unsubscription happens by sending an UNSUBSCRIBE frame when the flow collector's coroutine is cancelled, or
     * when a terminal operator such as [Flow.first][kotlinx.coroutines.flow.first] completes the flow from the
     * consumer's side.
     *
     * See the general [StompSession] documentation for more details about subscription flows, suspension and receipts.
     *
     * @see subscribeText
     * @see subscribeBinary
     */
    suspend fun subscribe(headers: StompSubscribeHeaders): Flow<StompFrame.Message>

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
     * If a RECEIPT frame is not received within the [configured time][StompConfig.disconnectTimeout], it may
     * be because the server closed the connection too quickly to send a RECEIPT frame, which is
     * [not considered an error](http://stomp.github.io/stomp-specification-1.2.html#Connection_Lingering).
     * That's why this function doesn't throw an exception in this case, it just returns normally.
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
 * Otherwise, this method suspends until the relevant RECEIPT frame is received from the server, and then returns
 * a [StompReceipt].
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeout],
 * a [LostReceiptException] is thrown.
 */
suspend fun StompSession.sendBinary(destination: String, body: ByteString?): StompReceipt? =
    send(StompSendHeaders(destination), body?.let { FrameBody.Binary(it) })

/**
 * Sends a SEND frame to the server at the given [destination] with the given textual [body].
 *
 * @return null right after sending the frame if auto-receipt is disabled.
 * Otherwise, this method suspends until the relevant RECEIPT frame is received from the server, and then returns
 * a [StompReceipt].
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeout],
 * a [LostReceiptException] is thrown.
 */
suspend fun StompSession.sendText(destination: String, body: String?): StompReceipt? =
    send(StompSendHeaders(destination), body?.let { FrameBody.Text(it) })

/**
 * Sends a SEND frame to the server at the given [destination] without body.
 *
 * @return null right after sending the frame if auto-receipt is disabled.
 * Otherwise, this method suspends until the relevant RECEIPT frame is received from the server, and then returns
 * a [StompReceipt].
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeout],
 * a [LostReceiptException] is thrown.
 */
suspend fun StompSession.sendEmptyMsg(destination: String): StompReceipt? = send(StompSendHeaders(destination), null)

/**
 * Subscribes and returns a [Flow] of [MESSAGE][StompFrame.Message] frames that unsubscribes automatically when the
 * collector is done or cancelled.
 * The returned flow can be collected only once.
 *
 * See the general [StompSession] documentation for more details about subscription flows, suspension and receipts.
 *
 * @see subscribeBinary
 * @see subscribeText
 */
suspend fun StompSession.subscribe(destination: String): Flow<StompFrame.Message> =
    subscribe(StompSubscribeHeaders(destination))

/**
 * Subscribes and returns a [Flow] of text message bodies that unsubscribes automatically when the collector is done or
 * cancelled.
 * The returned flow can be collected only once.
 *
 * The received MESSAGE frames' bodies are expected to be decodable as text: they must come from a textual web socket
 * frame, or their `content-type` header should start with `text/` or contain a `charset` parameter (see
 * [StompFrame.bodyAsText]).
 * If a received frame is not decodable as text, an exception is thrown.
 *
 * Frames without a body are indistinguishable from frames with a 0-length body, and therefore result in an empty
 * string in the subscription flow.
 *
 * See the general [StompSession] documentation for more details about subscription flows, suspension and receipts.
 */
suspend fun StompSession.subscribeText(destination: String): Flow<String> =
    subscribe(destination).map { it.bodyAsText }

/**
 * Subscribes and returns a [Flow] of binary message bodies that unsubscribes automatically when the collector is done
 * or cancelled.
 * The returned flow can be collected only once.
 *
 * Frames without a body are indistinguishable from frames with a 0-length body, and therefore result in an empty
 * [ByteArray] in the subscription flow.
 *
 * See the general [StompSession] documentation for more details about subscription flows, suspension and receipts.
 */
suspend fun StompSession.subscribeBinary(destination: String): Flow<ByteString> =
    subscribe(destination).map { it.body?.bytes ?: ByteString() }

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
suspend fun <T> StompSession.withTransaction(block: suspend StompSession.(transactionId: String) -> T): T {
    val transactionId = generateUuid()
    begin(transactionId)
    try {
        val result = TransactionStompSession(this, transactionId).block(transactionId)
        commit(transactionId)
        return result
    } catch (e: Exception) {
        try {
            abort(transactionId)
        } catch (abortException: Exception) {
            e.addSuppressed(abortException)
        }
        throw e
    }
}

/**
 * Executes the given block on this [StompSession], and [disconnects][StompSession.disconnect] from the session whether
 * the block terminated normally or exceptionally.
 */
suspend inline fun <S : StompSession, R> S.use(block: (S) -> R): R {
    try {
        return block(this)
    } finally {
        disconnect()
    }
}
