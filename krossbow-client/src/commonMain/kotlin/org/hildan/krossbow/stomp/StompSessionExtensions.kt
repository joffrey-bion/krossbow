package org.hildan.krossbow.stomp

import org.hildan.krossbow.stomp.headers.StompSendHeaders
import kotlin.reflect.KClass

/**
 * Sends a SEND frame to the server at the given [destination] with the given [body].
 *
 * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received form the server and returns a
 * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
 * [LostReceiptException] is thrown.
 *
 * If receipts are not enabled, this method sends the frame and immediately returns null.
 */
suspend fun StompSession.send(destination: String, body: ByteArray?): KrossbowReceipt? =
        send(StompSendHeaders(destination), body)

/**
 * Sends a SEND frame to the server at the given [destination] with the given [payload].
 *
 * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received from the server and returns a
 * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
 * [LostReceiptException] is thrown.
 *
 * If receipts are not enabled, this method sends the frame and immediately returns null.
 */
suspend fun <T : Any> StompSession.send(destination: String, payload: T? = null, payloadType: KClass<T>): KrossbowReceipt? =
    send(StompSendHeaders(destination), payload, payloadType)

/**
 * Sends a SEND frame to the server at the given [destination] with the given [payload].
 *
 * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received from the server and returns a
 * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
 * [LostReceiptException] is thrown.
 *
 * If receipts are not enabled, this method sends the frame and immediately returns null.
 */
suspend inline fun <reified T : Any> StompSession.send(destination: String, payload: T?): KrossbowReceipt? =
        send(destination, payload, T::class)

/**
 * Sends a SEND frame to the server at the given [destination] with no payload.
 *
 * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received from the server and returns a
 * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
 * [LostReceiptException] is thrown.
 *
 * If receipts are not enabled, this method sends the frame and immediately returns null.
 */
suspend fun StompSession.send(destination: String): KrossbowReceipt? = send(destination, null, Any::class)


/**
 * Subscribes to the given [destination], expecting objects of type [T]. The returned [KrossbowSubscription]
 * can be used to access the channel of received objects.
 *
 * A platform-specific deserializer is used to create instances of the given type from the body of every message
 * received on the created subscription.
 */
suspend inline fun <reified T : Any> StompSession.subscribe(destination: String): KrossbowSubscription<T> =
        subscribe(destination, T::class)
