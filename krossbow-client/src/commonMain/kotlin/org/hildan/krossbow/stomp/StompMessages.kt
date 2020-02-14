package org.hildan.krossbow.stomp

import org.hildan.krossbow.stomp.headers.StompMessageHeaders

/**
 * A STOMP receipt description, as specified in the STOMP specification.
 */
data class StompReceipt(
    /**
     * The value of the receipt header sent to the server, and returned in a RECEIPT frame.
     */
    val id: String
)

/**
 * A STOMP message.
 */
data class StompMessage<out T>(
    /**
     * Payload of the message. The type is converted to/from the frame's body by the platform-specific implementation.
     */
    val payload: T,
    /**
     * The headers of the message.
     */
    val headers: StompMessageHeaders
)
