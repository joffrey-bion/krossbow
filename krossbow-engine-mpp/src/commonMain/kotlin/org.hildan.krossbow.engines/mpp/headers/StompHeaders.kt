package org.hildan.krossbow.engines.mpp.headers

import org.hildan.krossbow.engines.MessageHeaders
import org.hildan.krossbow.engines.mpp.frame.HeartBeat
import org.hildan.krossbow.engines.mpp.frame.formatAsHeaderValue
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.ACCEPT_VERSION
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.ACK
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.DESTINATION
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.HEART_BEAT
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.HOST
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.ID
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.LOGIN
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.MESSAGE
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.MESSAGE_ID
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.PASSCODE
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.RECEIPT
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.RECEIPT_ID
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.SERVER
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.SESSION
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.SUBSCRIPTION
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.TRANSACTION
import org.hildan.krossbow.engines.mpp.headers.HeaderKeys.VERSION

object HeaderKeys {
    const val ACCEPT_VERSION = "accept-version"
    const val ACK = "ack"
    const val CONTENT_LENGTH = "content-length"
    const val DESTINATION = "destination"
    const val HEART_BEAT = "heart-beat"
    const val HOST = "host"
    const val ID = "id"
    const val LOGIN = "login"
    const val MESSAGE = "message"
    const val MESSAGE_ID = "message-id"
    const val PASSCODE = "passcode"
    const val RECEIPT = "receipt"
    const val RECEIPT_ID = "receipt-id"
    const val SERVER = "server"
    const val SESSION = "session"
    const val SUBSCRIPTION = "subscription"
    const val TRANSACTION = "transaction"
    const val VERSION = "version"
}

enum class AckMode(val headerValue: String) {
    AUTO("auto"),
    CLIENT("client"),
    CLIENT_INDIVIDUAL("client-individual");

    companion object {
        fun fromHeader(value: String) = values().first { it.headerValue == value }
    }
}

interface StompHeaders : Map<String, String>, MessageHeaders

internal data class SimpleStompHeaders(
    private val headers: Map<String, String>
) : StompHeaders, Map<String, String> by headers

fun Map<String, String>.asStompHeaders(): StompHeaders = SimpleStompHeaders(this)

fun headersOf(
    vararg pairs: Pair<String, String?>,
    customHeaders: Map<String, String> = emptyMap()
): StompHeaders {
    val headersMap = mutableMapOf<String, String>()
    pairs.forEach { (key, value) ->
        if (value != null) {
            headersMap[key] = value
        }
    }
    headersMap.putAll(customHeaders)
    return headersMap.asStompHeaders()
}

interface StandardHeaders : StompHeaders {
    val contentLength: Long?
    val contentType: String?
    val receipt: String?
}

class StompConnectHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val host: String by header()
    val acceptVersion: List<String> by acceptVersionHeader()
    val login: String? by optionalHeader()
    val passcode: String? by optionalHeader()
    val heartBeat: HeartBeat? by heartBeatHeader()

    constructor(
        host: String,
        acceptVersion: List<String> = listOf("1.2"),
        login: String? = null,
        passcode: String? = null,
        heartBeat: HeartBeat? = null
    ) : this(
        headersOf(
            HOST to host,
            ACCEPT_VERSION to acceptVersion.joinToString(","),
            LOGIN to login,
            PASSCODE to passcode,
            HEART_BEAT to heartBeat?.formatAsHeaderValue()
        )
    )
}

class StompConnectedHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val version: String by header()
    val session: String? by optionalHeader()
    val server: String? by optionalHeader()
    val heartBeat: HeartBeat? by heartBeatHeader()

    constructor(
        version: String,
        session: String? = null,
        server: String? = null,
        heartBeat: HeartBeat? = null
    ) : this(
        headersOf(
            VERSION to version, SESSION to session, SERVER to server, HEART_BEAT to heartBeat?.formatAsHeaderValue()
        )
    )
}

class StompSendHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val destination: String by header()
    val transaction: String? by optionalHeader()

    constructor(
        destination: String,
        transaction: String? = null,
        customHeaders: Map<String, String> = emptyMap()
    ) : this(
        headersOf(
            DESTINATION to destination, TRANSACTION to transaction, customHeaders = customHeaders
        )
    )
}

class StompSubscribeHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val destination: String by header()
    val id: String by header()
    val ack: AckMode by optionalHeader(default = AckMode.AUTO) { AckMode.fromHeader(it) }

    constructor(
        destination: String,
        id: String,
        ack: AckMode = AckMode.AUTO
    ) : this(
        headersOf(
            DESTINATION to destination,
            ID to id,
            ACK to ack.headerValue
        )
    )
}

class StompUnsubscribeHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val id: String by header()

    constructor(id: String) : this(headersOf(ID to id))
}

class StompDisconnectHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val receipt: String? by optionalHeader()

    constructor(receipt: String? = null) : this(headersOf(RECEIPT to receipt))
}

class StompAckHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val id: String by header()
    val transaction: String? by optionalHeader()

    constructor(id: String, transaction: String? = null) : this(
        headersOf(ID to id, TRANSACTION to transaction)
    )
}

class StompNackHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val id: String by header()
    val transaction: String? by optionalHeader()

    constructor(id: String, transaction: String? = null) : this(
        headersOf(ID to id, TRANSACTION to transaction)
    )
}

class StompBeginHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val transaction: String by header()

    constructor(transaction: String) : this(headersOf(TRANSACTION to transaction))
}

class StompCommitHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val transaction: String by header()

    constructor(transaction: String) : this(headersOf(TRANSACTION to transaction))
}

class StompAbortHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val transaction: String by header()

    constructor(transaction: String) : this(headersOf(TRANSACTION to transaction))
}

class StompMessageHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val destination: String by header()
    val messageId: String by header(MESSAGE_ID)
    val subscription: String by header()
    val ack: String? by optionalHeader()

    constructor(
        destination: String,
        messageId: String,
        subscription: String,
        ack: String? = null,
        customHeaders: Map<String, String> = emptyMap()
    ) : this(
        headersOf(
            DESTINATION to destination,
            MESSAGE_ID to messageId,
            SUBSCRIPTION to subscription,
            ACK to ack,
            customHeaders = customHeaders
        )
    )
}

class StompReceiptHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val receiptId: String by header(RECEIPT_ID)

    constructor(receiptId: String) : this(headersOf(RECEIPT_ID to receiptId))
}

class StompErrorHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val message: String? by optionalHeader()

    constructor(
        message: String? = null,
        customHeaders: Map<String, String> = emptyMap()
    ) : this(
        headersOf(
            MESSAGE to message, customHeaders = customHeaders
        )
    )
}
