package org.hildan.krossbow.stomp.frame

enum class StompCommand(
    val text: String,
    val supportsHeaderEscapes: Boolean = true
) {
    STOMP("STOMP"),
    // The CONNECT and CONNECTED frames do not escape the carriage return, line feed or colon octets
    // in order to remain backward compatible with STOMP 1.0
    // https://stomp.github.io/stomp-specification-1.2.html#Value_Encoding
    CONNECT("CONNECT", supportsHeaderEscapes = false),
    CONNECTED("CONNECTED", supportsHeaderEscapes = false),
    SEND("SEND"),
    SUBSCRIBE("SUBSCRIBE"),
    UNSUBSCRIBE("UNSUBSCRIBE"),
    ACK("ACK"),
    NACK("NACK"),
    BEGIN("BEGIN"),
    COMMIT("COMMIT"),
    ABORT("ABORT"),
    DISCONNECT("DISCONNECT"),
    MESSAGE("MESSAGE"),
    RECEIPT("RECEIPT"),
    ERROR("ERROR");

    companion object {
        private val valuesByText = values().associateBy { it.text }

        fun parse(text: String) = valuesByText[text] ?: throw InvalidStompCommandException(text)
    }
}

class InvalidStompCommandException(val invalidText: String) : Exception("Unknown STOMP command '$invalidText'")
