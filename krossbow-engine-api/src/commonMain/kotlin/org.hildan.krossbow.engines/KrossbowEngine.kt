package org.hildan.krossbow.engines

import kotlin.reflect.KClass

/**
 * Creates an instance of [KrossbowClient] based on the given [KrossbowEngine]. The provided configuration function
 * is applied to the newly created client.
 */
@Suppress("FunctionName")
fun KrossbowClient(engine: KrossbowEngine, configure: KrossbowConfig.() -> Unit = {}): KrossbowClient {
    val config = KrossbowConfig().apply { configure() }
    return engine.createClient(config)
}

/**
 * An interface to define how to create a [KrossbowClient]. Implementations can build platform-specific clients.
 */
interface KrossbowEngine {

    /**
     * Creates a [KrossbowClient] based on the given config. Implementations SHOULD implement all supported
     * configuration, and MAY ignore configuration that are not supported by the underlying websocket STOMP client.
     */
    fun createClient(config: KrossbowConfig): KrossbowClient
}

/**
 * A STOMP client interface based on Websockets. The client is used to connect to the server and create a
 * [KrossbowSession]. Then, most of the STOMP interactions are done through the [KrossbowSession].
 */
interface KrossbowClient {

    /**
     * Connects to the given WebSocket [url] and to the STOMP session, and returns after receiving the CONNECTED frame.
     */
    suspend fun connect(url: String, login: String? = null, passcode: String? = null): KrossbowSession
}

/**
 * Connects to the given [url] and executes the given [block] with the created session. The session is then
 * automatically closed at the end of the block.
 */
suspend fun KrossbowClient.useSession(
    url: String,
    login: String? = null,
    passcode: String? = null,
    block: suspend KrossbowSession.() -> Unit
) {
    val session = connect(url, login, passcode)
    try {
        session.block()
    } finally {
        session.disconnect()
    }
}

/**
 * An adapter for Krossbow sessions in a platform-specific engine. Is can be used to easily create an actual
 * [KrossbowSession].
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
    suspend fun send(destination: String, body: Any? = null): KrossbowReceipt?

    /**
     * Subscribes to the given [destination], expecting objects of type [clazz]. Empty payloads are represented by the
     * [Unit] type.
     *
     * A platform-specific deserializer is used to create instances of the given [clazz] from the body of every message
     * received on the created subscription.
     *
     * The subscription callbacks are adapted to the coroutines model by the actual [KrossbowSession].
     */
    suspend fun <T : Any> subscribe(
        destination: String,
        clazz: KClass<T>,
        callbacks: SubscriptionCallbacks<T>
    ): KrossbowEngineSubscription

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
 * An adapter for STOMP subscriptions in a platform-specific engine. Is can be used to easily create an actual
 * [KrossbowSubscription].
 */
data class KrossbowEngineSubscription(
    val id: String,
    val unsubscribe: suspend (UnsubscribeHeaders?) -> Unit
)

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
