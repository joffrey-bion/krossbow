package org.hildan.krossbow.engines

/**
 * An interface to define how to create a [KrossbowEngineClient]. Implementations can build platform-specific clients.
 */
interface KrossbowEngine {

    /**
     * Creates a [KrossbowEngineClient] based on the given config. Implementations SHOULD implement all supported
     * configuration, and MAY ignore features that are not supported by the underlying websocket STOMP client.
     */
    fun createClient(config: KrossbowEngineConfig): KrossbowEngineClient
}

/**
 * An engine-specific STOMP client.
 */
interface KrossbowEngineClient {

    /**
     * Connects to the given WebSocket [url] and to the STOMP session, and returns after receiving the CONNECTED frame.
     */
    suspend fun connect(url: String, login: String? = null, passcode: String? = null): KrossbowEngineSession
}

/**
 * An engine-specific STOMP session.
 */
interface KrossbowEngineSession {

    /**
     * Sends a SEND frame to the server at the given [destination] with the given [body].
     *
     * If auto-receipt is enabled, this method suspends until a RECEIPT frame is received form the server and returns a
     * [KrossbowReceipt]. If no RECEIPT frame is received from the server in the configured time limit, a
     * [LostReceiptException] is thrown.
     *
     * If receipts are not enabled, this method sends the frame and immediately returns null.
     */
    suspend fun send(destination: String, body: ByteArray? = null): KrossbowReceipt?

    /**
     * Subscribes to the given [destination], expecting binary payloads.
     *
     * The given [callbacks] must be used to send the received messages. Payload conversions are handled by Krossbow.
     *
     * Implementations SHOULD suspend until the subscription is acknowledged by the server.
     */
    suspend fun subscribe(destination: String, callbacks: SubscriptionCallbacks<ByteArray>): KrossbowEngineSubscription

    /**
     * Subscribes to the given [destination], expecting empty payloads.
     *
     * The given [callbacks] must be used to send the received messages. Payload conversions are handled by Krossbow.
     *
     * Implementations SHOULD suspend until the subscription is acknowledged by the server.
     */
    suspend fun subscribeNoPayload(destination: String, callbacks: SubscriptionCallbacks<Unit>): KrossbowEngineSubscription

    /**
     * Sends a DISCONNECT frame to close the session, and closes the connection.
     *
     * Implementations SHOULD suspend until the disconnection is acknowledged by the server.
     */
    suspend fun disconnect()
}

/**
 * Used to bridge the callback-based platform-specific implementations with the coroutine-based Krossbow sessions.
 */
interface SubscriptionCallbacks<in T> {
    /**
     * Called on each received MESSAGE frame for this subscription.
     */
    suspend fun onReceive(message: KrossbowMessage<T>)

    /**
     * Called if an error occurs in the context of this subscription.
     */
    fun onError(throwable: Throwable)
}

/**
 * An adapter for STOMP subscriptions in a platform-specific engine.
 */
data class KrossbowEngineSubscription(
    /**
     * The subscription ID.
     */
    val id: String,
    /**
     * A function to call in order to unsubscribe from this subscription, optionally taking headers as parameter.
     */
    val unsubscribe: suspend (UnsubscribeHeaders?) -> Unit
)

/**
 * An exception thrown when something went wrong during the connection.
 */
class ConnectionException(message: String) : Exception(message)

/**
 * An exception thrown when a RECEIPT frame was expected from the server, but not received in the configured time limit.
 */
class LostReceiptException(
    /**
     * The value of the receipt header sent to the server, and expected in a RECEIPT frame.
     */
    val receiptId: String
) : Exception("No RECEIPT frame received for receiptId '$receiptId' within the configured time limit")

/**
 * An exception thrown when a MESSAGE frame does not contain the expected payload type.
 */
class InvalidFramePayloadException(message: String) : Exception(message)
