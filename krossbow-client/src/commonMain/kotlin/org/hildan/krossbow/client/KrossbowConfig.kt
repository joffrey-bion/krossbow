package org.hildan.krossbow.client

import org.hildan.krossbow.client.converters.KotlinxSerialization
import org.hildan.krossbow.engines.HeartBeat
import org.hildan.krossbow.engines.KrossbowEngineConfig
import org.hildan.krossbow.engines.KrossbowMessage
import org.hildan.krossbow.engines.LostReceiptException
import kotlin.reflect.KClass

/**
 * Configuration of the STOMP [KrossbowClient].
 */
data class KrossbowConfig(
    /**
     * The heartbeat to use during STOMP sessions.
     */
    override var heartBeat: HeartBeat = HeartBeat(),
    /**
     * Whether to automatically attach a `receipt` header to the sent messages in order to track receipts.
     */
    override var autoReceipt: Boolean = false,
    /**
     * Defines how long to wait for a RECEIPT frame from the server before throwing a [LostReceiptException].
     * Only crashes when a `receipt` header was actually present in the sent frame (and thus a RECEIPT was expected).
     * Such header is always present if [autoReceipt] is enabled.
     */
    override var receiptTimeLimit: Long = 15000,
    /**
     * Used for conversion of message payloads to Kotlin objects.
     */
    var messageConverter: MessageConverter = KotlinxSerialization.JsonConverter()
) : KrossbowEngineConfig

interface MessageConverter {

    fun <T : Any> convertFromBytes(message: KrossbowMessage<ByteArray>, clazz: KClass<T>): KrossbowMessage<T>

    fun <T : Any> convertToBytes(value: T, clazz: KClass<T>): ByteArray
}
