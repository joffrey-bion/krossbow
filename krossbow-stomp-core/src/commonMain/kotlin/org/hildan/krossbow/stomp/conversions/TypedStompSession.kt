package org.hildan.krossbow.stomp.conversions

import kotlinx.coroutines.flow.Flow
import org.hildan.krossbow.stomp.LostReceiptException
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import kotlin.reflect.KClass

/**
 * A [StompSession] with additional methods to serialize/deserialize message bodies based on a [KClass].
 */
interface TypedStompSession : StompSession {

    /**
     * Sends a SEND frame to the server with the given [headers] and the given [body], converted appropriately based
     * on [bodyType].
     *
     * @return null right after sending the frame if auto-receipt is disabled and no receipt header is provided.
     * Otherwise, this method suspends until the relevant RECEIPT frame is received from the server, and then returns
     * a [StompReceipt].
     * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeout],
     * a [LostReceiptException] is thrown.
     */
    suspend fun <T> convertAndSend(headers: StompSendHeaders, body: T, bodyType: KTypeRef<T>): StompReceipt?

    /**
     * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the
     * collector is done or cancelled.
     * The returned flow can be collected only once.
     *
     * The received [MESSAGE][StompFrame.Message] frames are converted into instances of [T], but the exact conversion
     * is implementation-dependent.
     * In particular, message frames without a body can be processed in different ways depending on the implementation.
     * Implementations can choose to throw an exception, skip such frames, or convert them to null (if T is nullable).
     *
     * See the general [StompSession] documentation for more details about subscription flows and receipts.
     */
    suspend fun <T> subscribe(headers: StompSubscribeHeaders, type: KTypeRef<T>): Flow<T>
}

/**
 * Sends a SEND frame to the server with the given [headers] and [body], converted appropriately.
 *
 * @return null right after sending the frame if auto-receipt is disabled.
 * Otherwise, this method suspends until the relevant RECEIPT frame is received from the server, and then returns
 * a [StompReceipt].
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeout],
 * a [LostReceiptException] is thrown.
 */
suspend inline fun <reified T> TypedStompSession.convertAndSend(headers: StompSendHeaders, body: T): StompReceipt? =
    convertAndSend(headers, body, typeRefOf())

/**
 * Sends a SEND frame to the server at the given [destination] with the given [body], converted appropriately.
 *
 * @return null right after sending the frame if auto-receipt is disabled.
 * Otherwise, this method suspends until the relevant RECEIPT frame is received from the server, and then returns
 * a [StompReceipt].
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeout],
 * a [LostReceiptException] is thrown.
 */
suspend inline fun <reified T> TypedStompSession.convertAndSend(destination: String, body: T): StompReceipt? =
    convertAndSend(StompSendHeaders(destination), body)

/**
 * Subscribes to the given [destination] and returns a [Flow] of messages of type [T] that unsubscribes automatically
 * when the collector is done or cancelled.
 * The returned flow can be collected only once.
 *
 * The received [MESSAGE][StompFrame.Message] frames are converted to instances of [T], but the exact conversion
 * is implementation-dependent.
 * In particular, message frames without a body can be processed in different ways depending on the implementation.
 * Implementations can choose to throw an exception, skip such frames, or convert them to null (if T is nullable).
 *
 * See the general [StompSession] documentation for more details about subscription flows and receipts.
 */
suspend inline fun <reified T> TypedStompSession.subscribe(destination: String): Flow<T> =
    subscribe(StompSubscribeHeaders(destination), typeRefOf())
