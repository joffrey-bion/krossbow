package org.hildan.krossbow.stomp

import org.hildan.krossbow.stomp.headers.StompMessageHeaders

/**
 * A STOMP receipt description, as specified in the STOMP specification.
 */
data class KrossbowReceipt(
    /**
     * The value of the receipt header sent to the server, and returned in a RECEIPT frame.
     */
    val id: String
)

/**
 * A STOMP message.
 */
data class KrossbowMessage<out T>(
    /**
     * Payload of the message. The type is converted to/from the frame's body by the platform-specific implementation.
     */
    val payload: T,
    /**
     * The headers of the message.
     */
    val headers: StompMessageHeaders
)

/**
 * Transforms the payload type of this message using the given [transform]. Headers are preserved.
 */
fun <T, U> KrossbowMessage<T>.map(transform: (T) -> U): KrossbowMessage<U> =
        KrossbowMessage(transform(payload), headers)
