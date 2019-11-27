package org.hildan.krossbow.engines

/**
 * An interface to define how to create a [KrossbowEngineClient]. Implementations can build platform-specific clients.
 */
interface KrossbowEngine {

    /**
     * Creates a [KrossbowEngineClient] based on the given config. Implementations SHOULD implement all supported
     * configuration, and MAY ignore configuration that are not supported by the underlying websocket STOMP client.
     */
    fun createClient(config: KrossbowEngineConfig): KrossbowEngineClient
}

/**
 * An adapter STOMP clients in a platform-specific engine.
 */
interface KrossbowEngineClient {

    /**
     * Connects to the given WebSocket [url] and to the STOMP session, and returns after receiving the CONNECTED frame.
     */
    suspend fun connect(url: String, login: String? = null, passcode: String? = null): KrossbowEngineSession
}

/**
 * An adapter for STOMP sessions in a platform-specific engine.
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
     * Subscribes to the given [destination], expecting objects of type [T]. Empty payloads are accepted if the
     * provided [T] is [Unit].
     *
     * The subscription callbacks are adapted to the coroutines model by the actual [KrossbowSession].
     */
    suspend fun subscribe(destination: String, callbacks: SubscriptionCallbacks<ByteArray>): KrossbowEngineSubscription

    /**
     * Subscribes to the given [destination], expecting empty payloads.
     *
     * The subscription callbacks are adapted to the coroutines model by the actual [KrossbowSession].
     */
    suspend fun subscribeNoPayload(destination: String, callbacks: SubscriptionCallbacks<Unit>): KrossbowEngineSubscription

    /**
     * Sends a DISCONNECT frame to close the session, and closes the connection.
     */
    suspend fun disconnect()
}

/**
 * Used to bridge the callback-based platform-specific implementations with the coroutine-based [KrossbowSession]
 */
interface SubscriptionCallbacks<in T> {

    suspend fun onReceive(message: KrossbowMessage<T>)

    fun onError(throwable: Throwable)
}

/**
 * An adapter for STOMP subscriptions in a platform-specific engine.
 */
data class KrossbowEngineSubscription(
    val id: String,
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
