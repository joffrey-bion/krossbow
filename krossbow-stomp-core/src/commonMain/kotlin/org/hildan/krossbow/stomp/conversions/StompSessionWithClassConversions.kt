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
@Deprecated(
    message = "This interface uses KClass<T> to convey type information, which is not enough for generic types.",
    ReplaceWith("TypedStompSession", imports = ["org.hildan.krossbow.stomp.conversions.TypedStompSession"]),
)
interface StompSessionWithClassConversions : StompSession {

    /**
     * Sends a SEND frame to the server with the given [headers] and the given [body], converted appropriately based
     * on [bodyType].
     *
     * @return null right after sending the frame if auto-receipt is disabled and no receipt header is provided.
     * Otherwise this method suspends until the relevant RECEIPT frame is received from the server, and then returns
     * a [StompReceipt].
     * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeout],
     * a [LostReceiptException] is thrown.
     */
    suspend fun <T : Any> convertAndSend(
        headers: StompSendHeaders,
        body: T? = null,
        bodyType: KClass<T>,
    ): StompReceipt?

    /**
     * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the
     * collector is done or cancelled.
     * The returned flow can be collected only once.
     *
     * The received [MESSAGE][StompFrame.Message] frames are converted into instances of [T], but the exact conversion
     * is implementation-dependent.
     * Message frames without a body MAY be skipped or result in an exception depending on the implementation.
     *
     * See the general [StompSession] documentation for more details about subscription flows and receipts.
     */
    suspend fun <T : Any> subscribe(headers: StompSubscribeHeaders, clazz: KClass<T>): Flow<T>

    /**
     * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the
     * collector is done or cancelled.
     * The returned flow can be collected only once.
     *
     * The received [MESSAGE][StompFrame.Message] frames are converted into instances of [T] or `null`, but the exact
     * conversion is implementation-dependent.
     * Message frames without a body MAY be skipped or result in an exception depending on the implementation, but
     * they usually should be translated to `null`.
     *
     * See the general [StompSession] documentation for more details about subscription flows and receipts.
     */
    suspend fun <T : Any> subscribeOptional(headers: StompSubscribeHeaders, clazz: KClass<T>): Flow<T?>
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
suspend inline fun <reified T : Any> StompSessionWithClassConversions.convertAndSend(
    headers: StompSendHeaders,
    body: T? = null,
): StompReceipt? = convertAndSend(headers, body, T::class)

/**
 * Sends a SEND frame to the server at the given [destination] with the given [body], converted appropriately.
 *
 * @return null right after sending the frame if auto-receipt is disabled.
 * Otherwise, this method suspends until the relevant RECEIPT frame is received from the server, and then returns
 * a [StompReceipt].
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeout],
 * a [LostReceiptException] is thrown.
 */
@Deprecated(
    "This overload will be removed in a future release, prefer the reified version without bodyType argument",
    ReplaceWith("this.convertAndSend(destination, body)"),
)
suspend fun <T : Any> StompSessionWithClassConversions.convertAndSend(
    destination: String,
    body: T? = null,
    bodyType: KClass<T>,
): StompReceipt? = convertAndSend(StompSendHeaders(destination), body, bodyType)

/**
 * Sends a SEND frame to the server at the given [destination] with the given [body], converted appropriately.
 *
 * @return null right after sending the frame if auto-receipt is disabled.
 * Otherwise, this method suspends until the relevant RECEIPT frame is received from the server, and then returns
 * a [StompReceipt].
 * If no RECEIPT frame is received from the server in the configured [time limit][StompConfig.receiptTimeout],
 * a [LostReceiptException] is thrown.
 */
suspend inline fun <reified T : Any> StompSessionWithClassConversions.convertAndSend(
    destination: String,
    body: T?,
): StompReceipt? = convertAndSend(StompSendHeaders(destination), body)

/**
 * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the
 * collector is done or cancelled.
 * The returned flow can be collected only once.
 *
 * The received [MESSAGE][StompFrame.Message] frames are converted to instances of [T], but the exact conversion
 * is implementation-dependent.
 * Message frames without a body MAY be skipped or result in an exception depending on the implementation.
 *
 * See the general [StompSession] documentation for more details about subscription flows and receipts.
 */
suspend fun <T : Any> StompSessionWithClassConversions.subscribe(destination: String, clazz: KClass<T>): Flow<T> =
    subscribe(StompSubscribeHeaders(destination), clazz)

/**
 * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the
 * collector is done or cancelled.
 * The returned flow can be collected only once.
 *
 * The received [MESSAGE][StompFrame.Message] frames are converted to instances of [T] or `null`, but the exact
 * conversion is implementation-dependent.
 * Message frames without a body MAY be skipped or result in an exception depending on the implementation, they
 * don't have to be translated into null values.
 * Conversely, message frames with a non-empty body may result in null values in the flow depending on the
 * implementation.
 *
 * See the general [StompSession] documentation for more details about subscription flows and receipts.
 */
suspend fun <T : Any> StompSessionWithClassConversions.subscribeOptional(
    destination: String,
    clazz: KClass<T>,
): Flow<T?> = subscribeOptional(StompSubscribeHeaders(destination), clazz)

/**
 * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the
 * collector is done or cancelled.
 * The returned flow can be collected only once.
 *
 * The received [MESSAGE][StompFrame.Message] frames are converted to instances of [T], but the exact conversion
 * is implementation-dependent.
 * Message frames without a body MAY be skipped or result in an exception depending on the implementation.
 *
 * See the general [StompSession] documentation for more details about subscription flows and receipts.
 */
suspend inline fun <reified T : Any> StompSessionWithClassConversions.subscribe(destination: String): Flow<T> =
    subscribe(destination, T::class)

/**
 * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the
 * collector is done or cancelled.
 * The returned flow can be collected only once.
 *
 * The received [MESSAGE][StompFrame.Message] frames are converted to instances of [T] or `null`, but the exact
 * conversion is implementation-dependent.
 * Message frames without a body MAY be skipped or result in an exception depending on the implementation, they
 * don't have to be translated into null values.
 * Conversely, message frames with a non-empty body may result in null values in the flow depending on the
 * implementation.
 *
 * See the general [StompSession] documentation for more details about subscription flows and receipts.
 */
suspend inline fun <reified T : Any> StompSessionWithClassConversions.subscribeOptional(destination: String): Flow<T?> =
    subscribeOptional(destination, T::class)
