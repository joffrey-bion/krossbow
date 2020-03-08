package org.hildan.krossbow.stomp.conversions

import org.hildan.krossbow.stomp.LostReceiptException
import org.hildan.krossbow.stomp.StompReceipt
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.StompSubscription
import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import kotlin.reflect.KClass

/**
 * An extended [StompSession] that provides methods to serialize/deserialize message bodies using class-based
 * reflection.
 */
interface StompSessionWithReflection : StompSession {

    /**
     * Sends a SEND frame to the server with the given [headers] and the given [body], converted appropriately.
     *
     * @return null (immediately) if auto-receipt is disabled and no receipt header is provided. Otherwise this method
     * suspends until the relevant RECEIPT frame is received from the server, and then returns a [StompReceipt].
     */
    suspend fun <T : Any> convertAndSend(
        headers: StompSendHeaders,
        body: T? = null,
        bodyType: KClass<T>
    ): StompReceipt?

    /**
     * Subscribes to the given [destination], converting received messages into objects of type [T]. The returned
     * [StompSubscription] can be used to access the channel of received objects and unsubscribe.
     *
     * If auto-receipt is enabled or if a non-null [receiptId] is provided, this method suspends until the relevant
     * RECEIPT frame is received from the server. If no RECEIPT frame is received from the server
     * in the configured [time limit][StompConfig.receiptTimeoutMillis], a [LostReceiptException] is thrown.
     *
     * If auto-receipt is disabled and no [receiptId] is provided, this method returns immediately.
     */
    suspend fun <T : Any> subscribe(
        destination: String,
        clazz: KClass<T>,
        receiptId: String? = null
    ): StompSubscription<T>
}

/**
 * Sends a SEND frame to the server at the given [destination] with the given [body], converted appropriately.
 *
 * Please refer to [StompSession.send] for details about how receipts are handled.
 */
suspend fun <T : Any> StompSessionWithReflection.convertAndSend(
    destination: String,
    body: T? = null,
    bodyType: KClass<T>
): StompReceipt? = convertAndSend(StompSendHeaders(destination), body, bodyType)

/**
 * Sends a SEND frame to the server at the given [destination] with the given [body], converted appropriately.
 *
 * Please refer to [StompSession.send] for details about how receipts are handled.
 */
suspend inline fun <reified T : Any> StompSessionWithReflection.convertAndSend(destination: String, body: T?): StompReceipt? =
        convertAndSend(destination, body, T::class)

/**
 * Subscribes to the given [destination], expecting objects of type [T]. The returned [StompSubscription]
 * can be used to access the channel of received objects.
 */
suspend inline fun <reified T : Any> StompSessionWithReflection.subscribe(
    destination: String,
    receiptId: String? = null
): StompSubscription<T> = subscribe(destination, T::class, receiptId)
