package org.hildan.krossbow.stomp.conversions.kxserialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.getContextualOrDefault
import org.hildan.krossbow.stomp.LostReceiptException
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.StompSubscription
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.headers.StompSendHeaders

/**
 * A [StompSession] with additional methods to serialize/deserialize message bodies using Kotlinx Serialization.
 */
interface StompSessionWithKxSerialization : StompSession {

    val context: SerialModule

    /**
     * Sends a SEND frame to the server with the given [headers] and the given [body], converted appropriately using
     * the provided [serializer].
     *
     * @return null (immediately) if auto-receipt is disabled and no receipt header is provided. Otherwise this method
     * suspends until the relevant RECEIPT frame is received from the server, and then returns a [StompReceipt].
     */
    suspend fun <T : Any> convertAndSend(
        headers: StompSendHeaders,
        body: T? = null,
        serializer: SerializationStrategy<T>
    ): StompReceipt?

    /**
     * Subscribes to the given [destination], converting received messages into objects of type [T] using the given
     * [deserializer].
     * Empty messages are not allowed and result in an exception in the messages channel.
     * The returned [StompSubscription] can be used to access the channel of received objects and unsubscribe.
     *
     * If auto-receipt is enabled or if a non-null [receiptId] is provided, this method suspends until the relevant
     * RECEIPT frame is received from the server. If no RECEIPT frame is received from the server
     * in the configured [time limit][StompConfig.receiptTimeoutMillis], a [LostReceiptException] is thrown.
     *
     * If auto-receipt is disabled and no [receiptId] is provided, this method returns immediately.
     */
    suspend fun <T : Any> subscribe(
        destination: String,
        deserializer: DeserializationStrategy<T>,
        receiptId: String? = null
    ): StompSubscription<T>

    /**
     * Subscribes to the given [destination], converting received messages into objects of type [T].
     * In this variant, empty messages are allowed and result in a null value in the messages channel.
     * The returned [StompSubscription] can be used to access the channel of received objects and unsubscribe.
     *
     * If auto-receipt is enabled or if a non-null [receiptId] is provided, this method suspends until the relevant
     * RECEIPT frame is received from the server. If no RECEIPT frame is received from the server
     * in the configured [time limit][StompConfig.receiptTimeoutMillis], a [LostReceiptException] is thrown.
     *
     * If auto-receipt is disabled and no [receiptId] is provided, this method returns immediately.
     */
    suspend fun <T : Any> subscribeOptional(
        destination: String,
        deserializer: DeserializationStrategy<T>,
        receiptId: String? = null
    ): StompSubscription<T?>
}

/**
 * Sends a SEND frame to the given [destination] with the given [body], converted appropriately using the provided
 * [serializer].
 *
 * @return null (immediately) if auto-receipt is disabled and no receipt header is provided. Otherwise this method
 * suspends until the relevant RECEIPT frame is received from the server, and then returns a [StompReceipt].
 */
suspend fun <T : Any> StompSessionWithKxSerialization.convertAndSend(
    destination: String,
    body: T? = null,
    serializer: SerializationStrategy<T>
): StompReceipt? = convertAndSend(StompSendHeaders(destination), body, serializer)

/**
 * Sends a SEND frame to the server with the given [headers] and the given [body], converted appropriately.
 *
 * This overload uses reflection to find the relevant deserializer for [T]. This has limited support in JavaScript
 * and may break on generic types.
 *
 * @return null (immediately) if auto-receipt is disabled and no receipt header is provided. Otherwise this method
 * suspends until the relevant RECEIPT frame is received from the server, and then returns a [StompReceipt].
 */
@ImplicitReflectionSerializer
suspend inline fun <reified T : Any> StompSessionWithKxSerialization.convertAndSend(
    headers: StompSendHeaders,
    body: T? = null
): StompReceipt? {
    val serializer = context.getContextualOrDefault(T::class)
    return convertAndSend(headers, body, serializer)
}

/**
 * Sends a SEND frame to the given [destination] with the given [body], converted appropriately.
 *
 * This overload uses reflection to find the relevant deserializer for [T]. This has limited support in JavaScript
 * and may break on generic types.
 *
 * @return null (immediately) if auto-receipt is disabled and no receipt header is provided. Otherwise this method
 * suspends until the relevant RECEIPT frame is received from the server, and then returns a [StompReceipt].
 */
@ImplicitReflectionSerializer
suspend inline fun <reified T : Any> StompSessionWithKxSerialization.convertAndSend(
    destination: String,
    body: T? = null
): StompReceipt? = convertAndSend(StompSendHeaders(destination), body)

/**
 * Subscribes to the given [destination], converting received messages into objects of type [T]. The returned
 * [StompSubscription] can be used to access the channel of received objects and unsubscribe.
 *
 * This overload uses reflection to find the relevant deserializer for [T]. This has limited support in JavaScript
 * and may break on generic types.
 *
 * If auto-receipt is enabled or if a non-null [receiptId] is provided, this method suspends until the relevant
 * RECEIPT frame is received from the server. If no RECEIPT frame is received from the server
 * in the configured [time limit][StompConfig.receiptTimeoutMillis], a [LostReceiptException] is thrown.
 *
 * If auto-receipt is disabled and no [receiptId] is provided, this method returns immediately.
 */
@ImplicitReflectionSerializer
suspend inline fun <reified T : Any> StompSessionWithKxSerialization.subscribe(
    destination: String,
    receiptId: String? = null
): StompSubscription<T> {
    val serializer = context.getContextualOrDefault(T::class)
    return subscribe(destination, serializer, receiptId)
}
