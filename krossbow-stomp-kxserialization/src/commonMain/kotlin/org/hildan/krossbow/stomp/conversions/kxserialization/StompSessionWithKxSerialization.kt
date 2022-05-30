package org.hildan.krossbow.stomp.conversions.kxserialization

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders

/**
 * A [StompSession] with additional methods to serialize/deserialize message bodies using Kotlinx Serialization.
 */
interface StompSessionWithKxSerialization : StompSession {

    /**
     * The Kotlinx Serialization's [SerializersModule] used to provide serializers at runtime.
     */
    val serializersModule: SerializersModule

    /**
     * Sends a SEND frame to the server with the given [headers] and the given [body], converted appropriately using
     * the provided [serializer].
     *
     * @return null (immediately) if auto-receipt is disabled and no receipt header is provided. Otherwise, this method
     * suspends until the relevant RECEIPT frame is received from the server, and then returns a [StompReceipt].
     */
    suspend fun <T : Any> convertAndSend(
        headers: StompSendHeaders,
        body: T? = null,
        serializer: SerializationStrategy<T>,
    ): StompReceipt?

    /**
     * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the collector is
     * done or cancelled.
     * The returned flow can be collected only once.
     *
     * The received [MESSAGE][StompFrame.Message] frames are converted into instances of [T] using the provided
     * Kotlinx Serialization's [deserializer].
     * Message frames without a body are not allowed and result in an exception in the flow's collector.
     *
     * See the general [StompSession] documentation for more details about subscription flows and receipts.
     */
    suspend fun <T : Any> subscribe(headers: StompSubscribeHeaders, deserializer: DeserializationStrategy<T>): Flow<T>

    /**
     * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the collector is
     * done or cancelled.
     * The returned flow can be collected only once.
     *
     * The received [MESSAGE][StompFrame.Message] frames are converted to instances of [T] using the provided Kotlinx
     * Serialization's [deserializer].
     * Message frames without a body are allowed and result in `null` values in the flow.
     *
     * See the general [StompSession] documentation for more details about subscription flows and receipts.
     */
    suspend fun <T : Any> subscribeOptional(
        headers: StompSubscribeHeaders,
        deserializer: DeserializationStrategy<T>,
    ): Flow<T?>
}

/**
 * Sends a SEND frame to the given [destination] with the given [body], converted appropriately using the provided
 * [serializer].
 *
 * @return null (immediately) if auto-receipt is disabled and no receipt header is provided. Otherwise, this method
 * suspends until the relevant RECEIPT frame is received from the server, and then returns a [StompReceipt].
 */
suspend fun <T : Any> StompSessionWithKxSerialization.convertAndSend(
    destination: String,
    body: T? = null,
    serializer: SerializationStrategy<T>,
): StompReceipt? = convertAndSend(StompSendHeaders(destination), body, serializer)

/**
 * Sends a SEND frame to the server with the given [headers] and the given [body], converted appropriately.
 *
 * This overload uses reflection to find the relevant deserializer for [T]. This has limited support in JavaScript
 * and may break on generic types.
 *
 * @return null (immediately) if auto-receipt is disabled and no receipt header is provided. Otherwise, this method
 * suspends until the relevant RECEIPT frame is received from the server, and then returns a [StompReceipt].
 */
suspend inline fun <reified T : Any> StompSessionWithKxSerialization.convertAndSend(
    headers: StompSendHeaders,
    body: T? = null,
): StompReceipt? {
    val serializer = serializersModule.serializer<T>()
    return convertAndSend(headers, body, serializer)
}

/**
 * Sends a SEND frame to the given [destination] with the given [body], converted appropriately.
 *
 * This overload uses reflection to find the relevant deserializer for [T]. This has limited support in JavaScript
 * and may break on generic types.
 *
 * @return null (immediately) if auto-receipt is disabled and no receipt header is provided. Otherwise, this method
 * suspends until the relevant RECEIPT frame is received from the server, and then returns a [StompReceipt].
 */
suspend inline fun <reified T : Any> StompSessionWithKxSerialization.convertAndSend(
    destination: String,
    body: T? = null,
): StompReceipt? = convertAndSend(StompSendHeaders(destination), body)

/**
 * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the collector is done
 * or cancelled.
 * The returned flow can be collected only once.
 *
 * The received [MESSAGE][StompFrame.Message] frames are converted to instances of [T] using the provided Kotlinx
 * Serialization's [deserializer].
 * Message frames without a body are not allowed and result in an exception in the flow's collector.
 *
 * See the general [StompSession] documentation for more details about subscription flows and receipts.
 */
suspend fun <T : Any> StompSessionWithKxSerialization.subscribe(
    destination: String,
    deserializer: DeserializationStrategy<T>,
): Flow<T> = subscribe(StompSubscribeHeaders(destination), deserializer)

/**
 * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the collector is done
 * or cancelled.
 * The returned flow can be collected only once.
 *
 * The received [MESSAGE][StompFrame.Message] frames are converted to instances of [T] using the provided Kotlinx
 * Serialization's [deserializer].
 * Message frames without a body are allowed and result in null values in the flow.
 *
 * See the general [StompSession] documentation for more details about subscription flows and receipts.
 */
suspend fun <T : Any> StompSessionWithKxSerialization.subscribeOptional(
    destination: String,
    deserializer: DeserializationStrategy<T>,
): Flow<T?> = subscribeOptional(StompSubscribeHeaders(destination), deserializer)

/**
 * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the collector is done
 * or cancelled.
 * The returned flow can be collected only once.
 *
 * The received [MESSAGE][StompFrame.Message] frames are converted to instances of [T] using a deserializer inferred
 * from the class of [T]. This has limited support in JavaScript and may break on generic types.
 * Message frames without a body are not allowed and result in an exception in the flow's collector.
 *
 * See the general [StompSession] documentation for more details about subscription flows and receipts.
 */
suspend inline fun <reified T : Any> StompSessionWithKxSerialization.subscribe(destination: String): Flow<T> {
    val serializer = serializersModule.serializer<T>()
    return subscribe(destination, serializer)
}

/**
 * Subscribes and returns a [Flow] of messages of type [T] that unsubscribes automatically when the collector is done
 * or cancelled.
 * The returned flow can be collected only once.
 *
 * The received [MESSAGE][StompFrame.Message] frames are converted to instances of [T] using a deserializer inferred
 * from the class of [T]. This has limited support in JavaScript and may break on generic types.
 * Message frames without a body are allowed and result in null values in the flow.
 *
 * See the general [StompSession] documentation for more details about subscription flows and receipts.
 */
suspend inline fun <reified T : Any> StompSessionWithKxSerialization.subscribeOptional(destination: String): Flow<T?> {
    val serializer = serializersModule.serializer<T>()
    return subscribeOptional(destination, serializer)
}
