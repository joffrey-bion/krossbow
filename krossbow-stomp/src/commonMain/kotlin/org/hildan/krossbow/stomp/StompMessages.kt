package org.hildan.krossbow.stomp

import org.hildan.krossbow.stomp.frame.FrameBody
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
     * The body of the message. The type is converted to/from the frame's body by the platform-specific implementation.
     */
    val payload: T,
    /**
     * The headers of the message.
     */
    val headers: StompMessageHeaders
)

data class FrameContent internal constructor(
    val body: FrameBody?,
    val contentType: String?,
    val contentLength: Int,
    val customHeaders: Map<String, String>
) {
    companion object {

        fun withBody(
            body: FrameBody,
            contentType: String,
            contentLength: Int = body.bytes.size,
            customHeaders: Map<String, String> = emptyMap()
        ) = FrameContent(body, contentType, contentLength, customHeaders)

        fun withoutBody(customHeaders: Map<String, String> = emptyMap()) = FrameContent(
            body = null,
            contentType = null,
            contentLength = 0,
            customHeaders = customHeaders
        )
    }
}
