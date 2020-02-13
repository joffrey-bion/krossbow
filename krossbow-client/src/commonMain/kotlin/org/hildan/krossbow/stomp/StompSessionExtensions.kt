package org.hildan.krossbow.stomp

import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import kotlin.reflect.KClass

/**
 * Sends a SEND frame to the server at the given [destination] with no payload.
 *
 * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received from the server and returns a
 * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
 * [LostReceiptException] is thrown.
 *
 * If receipts are not enabled, this method sends the frame and immediately returns null.
 */
suspend fun StompSession.send(destination: String): KrossbowReceipt? = send(StompSendHeaders(destination), null)

/**
 * Sends a SEND frame to the server at the given [destination] with the given binary [body].
 *
 * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received form the server and returns a
 * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
 * [LostReceiptException] is thrown.
 *
 * If receipts are not enabled, this method sends the frame and immediately returns null.
 */
suspend fun StompSession.sendBinary(destination: String, body: ByteArray?): KrossbowReceipt? =
        send(StompSendHeaders(destination), body?.let { FrameBody.Binary(it) })

/**
 * Sends a SEND frame to the server at the given [destination] with the given textual [body].
 *
 * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received form the server and returns a
 * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
 * [LostReceiptException] is thrown.
 *
 * If receipts are not enabled, this method sends the frame and immediately returns null.
 */
suspend fun StompSession.sendText(destination: String, body: String?): KrossbowReceipt? =
        send(StompSendHeaders(destination), body?.let { FrameBody.Text(it) })

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
 * Subscribes to the given [destination], expecting objects of type [T]. The returned [KrossbowSubscription]
 * can be used to access the channel of received objects.
 *
 * A platform-specific deserializer is used to create instances of the given type from the body of every message
 * received on the created subscription.
 */
suspend inline fun <reified T : Any> StompSession.subscribe(destination: String): KrossbowSubscription<T> =
        subscribe(destination, T::class)
