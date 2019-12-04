package org.hildan.krossbow.client

import org.hildan.krossbow.client.converters.KotlinxSerialization
import org.hildan.krossbow.client.converters.MessageConverter
import org.hildan.krossbow.engines.HeartBeat
import org.hildan.krossbow.engines.KrossbowEngineConfig

/**
 * Configuration for the STOMP [KrossbowClient].
 */
data class KrossbowConfig(
    override var heartBeat: HeartBeat = HeartBeat(),
    override var autoReceipt: Boolean = false,
    override var receiptTimeLimit: Long = 15000,
    /**
     * Used for conversion of message payloads to Kotlin objects. Defaults to JSON conversion using Kotlinx
     * Serialization.
     */
    var messageConverter: MessageConverter = KotlinxSerialization.JsonConverter()
) : KrossbowEngineConfig
