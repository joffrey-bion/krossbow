package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.headers.HeaderKeys.ACCEPT_VERSION
import org.hildan.krossbow.stomp.headers.HeaderKeys.ACK
import org.hildan.krossbow.stomp.headers.HeaderKeys.CONTENT_LENGTH
import org.hildan.krossbow.stomp.headers.HeaderKeys.CONTENT_TYPE
import org.hildan.krossbow.stomp.headers.HeaderKeys.DESTINATION
import org.hildan.krossbow.stomp.headers.HeaderKeys.HEART_BEAT
import org.hildan.krossbow.stomp.headers.HeaderKeys.HOST
import org.hildan.krossbow.stomp.headers.HeaderKeys.ID
import org.hildan.krossbow.stomp.headers.HeaderKeys.LOGIN
import org.hildan.krossbow.stomp.headers.HeaderKeys.MESSAGE
import org.hildan.krossbow.stomp.headers.HeaderKeys.MESSAGE_ID
import org.hildan.krossbow.stomp.headers.HeaderKeys.PASSCODE
import org.hildan.krossbow.stomp.headers.HeaderKeys.RECEIPT
import org.hildan.krossbow.stomp.headers.HeaderKeys.RECEIPT_ID
import org.hildan.krossbow.stomp.headers.HeaderKeys.SERVER
import org.hildan.krossbow.stomp.headers.HeaderKeys.SESSION
import org.hildan.krossbow.stomp.headers.HeaderKeys.SUBSCRIPTION
import org.hildan.krossbow.stomp.headers.HeaderKeys.TRANSACTION
import org.hildan.krossbow.stomp.headers.HeaderKeys.VERSION

object HeaderKeys {
    const val ACCEPT_VERSION = "accept-version"
    const val ACK = "ack"
    const val CONTENT_LENGTH = "content-length"
    const val CONTENT_TYPE = "content-type"
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

/**
 * Represents possible values for the
 * [`ack` header](https://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE_ack_Header).
 */
enum class AckMode(val headerValue: String) {
    /**
     * When the ack mode is auto, then the client does not need to send the server ACK frames for the messages it
     * receives. The server will assume the client has received the message as soon as it sends it to the client. This
     * acknowledgment mode can cause messages being transmitted to the client to get dropped.
     */
    AUTO("auto"),
    /**
     * When the ack mode is client, then the client MUST send the server ACK frames for the messages it processes. If
     * the connection fails before a client sends an ACK frame for the message the server will assume the message has
     * not been processed and MAY redeliver the message to another client. The ACK frames sent by the client will be
     * treated as a cumulative acknowledgment. This means the acknowledgment operates on the message specified in the
     * ACK frame and all messages sent to the subscription before the ACK'ed message.
     */
    CLIENT("client"),
    /**
     * When the ack mode is client-individual, the acknowledgment operates just like the client acknowledgment mode
     * except that the ACK or NACK frames sent by the client are not cumulative. This means that an ACK or NACK frame
     * for a subsequent message MUST NOT cause a previous message to get acknowledged.
     */
    CLIENT_INDIVIDUAL("client-individual");

    companion object {
        fun fromHeader(value: String) = values().first { it.headerValue == value }
    }
}

interface StompHeaders : MutableMap<String, String> {

    var contentLength: Int?

    var contentType: String?

    var receipt: String?
}

private data class SimpleStompHeaders(
    private val headers: MutableMap<String, String>
) : StompHeaders, MutableMap<String, String> by headers {

    override var contentLength: Int? by mutableOptionalIntHeader(CONTENT_LENGTH)

    override var contentType: String? by mutableOptionalHeader(CONTENT_TYPE)

    override var receipt: String? by mutableOptionalHeader(RECEIPT)
}

internal fun MutableMap<String, String>.asStompHeaders(): StompHeaders = SimpleStompHeaders(this)

internal fun headersOf(
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
        version: String = "1.2",
        session: String? = null,
        server: String? = null,
        heartBeat: HeartBeat? = null
    ) : this(
        headersOf(
            VERSION to version,
            SESSION to session,
            SERVER to server,
            HEART_BEAT to heartBeat?.formatAsHeaderValue()
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
            DESTINATION to destination,
            TRANSACTION to transaction,
            customHeaders = customHeaders
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

    constructor(receipt: String? = null) : this(headersOf(RECEIPT to receipt))
}

class StompAckHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val id: String by header()
    val transaction: String? by optionalHeader()

    constructor(id: String, transaction: String? = null) : this(headersOf(ID to id, TRANSACTION to transaction))
}

class StompNackHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val id: String by header()
    val transaction: String? by optionalHeader()

    constructor(id: String, transaction: String? = null) : this(headersOf(ID to id, TRANSACTION to transaction))
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

data class StompMessageHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
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
    val receiptId: String? by optionalHeader(RECEIPT_ID)

    constructor(
        message: String? = null,
        receiptId: String? = null,
        customHeaders: Map<String, String> = emptyMap()
    ) : this(
        headersOf(
            MESSAGE to message,
            RECEIPT_ID to receiptId,
            customHeaders = customHeaders
        )
    )
}

private fun StompHeaders.acceptVersionHeader() = header(HeaderKeys.ACCEPT_VERSION) { it.split(',') }

private fun StompHeaders.heartBeatHeader() = optionalHeader(HeaderKeys.HEART_BEAT) { it.toHeartBeat() }

private fun String.toHeartBeat(): HeartBeat {
    val (minSendPeriod, expectedReceivePeriod) = split(',')
    return HeartBeat(
        minSendPeriodMillis = minSendPeriod.toInt(),
        expectedPeriodMillis = expectedReceivePeriod.toInt()
    )
}

private fun HeartBeat.formatAsHeaderValue() = "$minSendPeriodMillis,$expectedPeriodMillis"
