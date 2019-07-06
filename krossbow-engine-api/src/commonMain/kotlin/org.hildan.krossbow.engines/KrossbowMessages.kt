package org.hildan.krossbow.engines

// TODO handle headers typing for params and received frames
interface MessageHeaders

interface UnsubscribeHeaders

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
    val headers: MessageHeaders
)
