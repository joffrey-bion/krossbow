package org.hildan.krossbow.stomp.version

internal enum class StompVersion(
    /**
     * The value of this version in an `accept-version` header.
     */
    val headerValue: String,
    /**
     * Web socket subprotocol IDs as defined by IANA's
     * [WebSocket Subprotocol Name Registry](https://www.iana.org/assignments/websocket/websocket.xhtml#subprotocol-name).
     */
    val wsSubprotocolId: String,
) {
    V1_0(headerValue = "1.0", wsSubprotocolId = "v10.stomp"),
    V1_1(headerValue = "1.1", wsSubprotocolId = "v11.stomp"),
    V1_2(headerValue = "1.2", wsSubprotocolId = "v12.stomp");

    /**
     * Whether this version of the protocol supports virtual hosts and thus the `host` header in the CONNECT frame.
     */
    val supportsHostHeader: Boolean get() = this >= V1_1

    override fun toString(): String = headerValue

    companion object {
        val preferredOrder = listOf(V1_2, V1_1, V1_0)

        fun fromWsProtocol(protocol: String?): StompVersion? = entries.find { it.wsSubprotocolId == protocol }

        fun fromConnectedFrame(versionHeader: String): StompVersion =
            entries.find { it.headerValue == versionHeader } ?: error("unknown STOMP protocol version $versionHeader")
    }
}
