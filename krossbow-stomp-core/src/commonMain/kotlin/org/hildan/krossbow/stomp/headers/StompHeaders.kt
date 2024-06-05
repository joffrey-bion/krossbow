package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.headers.HeaderNames.ACCEPT_VERSION
import org.hildan.krossbow.stomp.headers.HeaderNames.ACK
import org.hildan.krossbow.stomp.headers.HeaderNames.CONTENT_LENGTH
import org.hildan.krossbow.stomp.headers.HeaderNames.CONTENT_TYPE
import org.hildan.krossbow.stomp.headers.HeaderNames.DESTINATION
import org.hildan.krossbow.stomp.headers.HeaderNames.HEART_BEAT
import org.hildan.krossbow.stomp.headers.HeaderNames.HOST
import org.hildan.krossbow.stomp.headers.HeaderNames.ID
import org.hildan.krossbow.stomp.headers.HeaderNames.LOGIN
import org.hildan.krossbow.stomp.headers.HeaderNames.MESSAGE
import org.hildan.krossbow.stomp.headers.HeaderNames.MESSAGE_ID
import org.hildan.krossbow.stomp.headers.HeaderNames.PASSCODE
import org.hildan.krossbow.stomp.headers.HeaderNames.RECEIPT
import org.hildan.krossbow.stomp.headers.HeaderNames.RECEIPT_ID
import org.hildan.krossbow.stomp.headers.HeaderNames.SERVER
import org.hildan.krossbow.stomp.headers.HeaderNames.SESSION
import org.hildan.krossbow.stomp.headers.HeaderNames.SUBSCRIPTION
import org.hildan.krossbow.stomp.headers.HeaderNames.TRANSACTION
import org.hildan.krossbow.stomp.headers.HeaderNames.VERSION
import org.hildan.krossbow.stomp.utils.generateUuid
import org.hildan.krossbow.stomp.version.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents the headers of a STOMP frame.
 */
interface StompHeaders : MutableMap<String, String> {

    var contentLength: Int?

    var contentType: String?

    var receipt: String?
}

private data class SimpleStompHeaders(
    private val headers: MutableMap<String, String>,
) : StompHeaders, MutableMap<String, String> by headers {

    override var contentLength: Int? by mutableOptionalIntHeader(CONTENT_LENGTH)

    override var contentType: String? by mutableOptionalHeader(CONTENT_TYPE)

    override var receipt: String? by mutableOptionalHeader(RECEIPT)
}

internal fun MutableMap<String, String>.asStompHeaders(): StompHeaders = SimpleStompHeaders(this)

internal fun headersOf(
    vararg pairs: Pair<String, String?>,
    customHeaders: Map<String, String> = emptyMap(),
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

data class StompConnectHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val host: String? by optionalHeader() // mandatory since 1.2, but forbidden in some 1.0 servers
    val acceptVersion: List<String> by acceptVersionHeader()
    val login: String? by optionalHeader()
    val passcode: String? by optionalHeader()
    val heartBeat: HeartBeat? by heartBeatHeader()

    constructor(
        host: String?,
        acceptVersion: List<String> = StompVersion.preferredOrder.map { it.headerValue },
        login: String? = null,
        passcode: String? = null,
        heartBeat: HeartBeat? = null,
        customHeaders: Map<String, String> = emptyMap(),
    ) : this(
        headersOf(
            HOST to host,
            ACCEPT_VERSION to acceptVersion.joinToString(","),
            LOGIN to login,
            PASSCODE to passcode,
            HEART_BEAT to heartBeat?.formatAsHeaderValue(),
            customHeaders = customHeaders,
        )
    )
}

data class StompConnectedHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val version: String by optionalHeader(default = "1.0") { it } // mandatory since 1.1, but not sent by 1.0 servers
    val session: String? by optionalHeader()
    val server: String? by optionalHeader()
    val heartBeat: HeartBeat? by heartBeatHeader()

    constructor(
        version: String = "1.2",
        session: String? = null,
        server: String? = null,
        heartBeat: HeartBeat? = null,
    ) : this(
        headersOf(
            VERSION to version,
            SESSION to session,
            SERVER to server,
            HEART_BEAT to heartBeat?.formatAsHeaderValue(),
        )
    )
}

data class StompSendHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val destination: String by header()
    var transaction: String? by mutableOptionalHeader()

    constructor(
        destination: String,
        transaction: String? = null,
        receipt: String? = null,
        customHeaders: Map<String, String> = emptyMap(),
    ) : this(
        headersOf(
            DESTINATION to destination,
            TRANSACTION to transaction,
            RECEIPT to receipt,
            customHeaders = customHeaders,
        )
    )
}

data class StompSubscribeHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val destination: String by header()
    val id: String by header()
    val ack: AckMode by optionalHeader(default = AckMode.AUTO) { AckMode.fromHeader(it) }

    constructor(
        destination: String,
        id: String = generateUuid(),
        ack: AckMode = AckMode.AUTO,
        receipt: String? = null,
        customHeaders: Map<String, String> = emptyMap(),
    ) : this(
        headersOf(
            DESTINATION to destination,
            ID to id,
            ACK to ack.headerValue,
            RECEIPT to receipt,
            customHeaders = customHeaders,
        )
    )
}

data class StompUnsubscribeHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val id: String by header()

    constructor(id: String) : this(headersOf(ID to id))
}

data class StompDisconnectHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {

    constructor(receipt: String? = null) : this(headersOf(RECEIPT to receipt))
}

data class StompAckHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val id: String by header()
    val transaction: String? by optionalHeader()

    constructor(id: String, transaction: String? = null) : this(headersOf(ID to id, TRANSACTION to transaction))
}

data class StompNackHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val id: String by header()
    val transaction: String? by optionalHeader()

    constructor(id: String, transaction: String? = null) : this(headersOf(ID to id, TRANSACTION to transaction))
}

data class StompBeginHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val transaction: String by header()

    constructor(transaction: String) : this(headersOf(TRANSACTION to transaction))
}

data class StompCommitHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val transaction: String by header()

    constructor(transaction: String) : this(headersOf(TRANSACTION to transaction))
}

data class StompAbortHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
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
        customHeaders: Map<String, String> = emptyMap(),
    ) : this(
        headersOf(
            DESTINATION to destination,
            MESSAGE_ID to messageId,
            SUBSCRIPTION to subscription,
            ACK to ack,
            customHeaders = customHeaders,
        )
    )
}

data class StompReceiptHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val receiptId: String by header(RECEIPT_ID)

    constructor(receiptId: String) : this(headersOf(RECEIPT_ID to receiptId))
}

data class StompErrorHeaders(private val rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val message: String? by optionalHeader()
    val receiptId: String? by optionalHeader(RECEIPT_ID)

    constructor(
        message: String? = null,
        receiptId: String? = null,
        customHeaders: Map<String, String> = emptyMap(),
    ) : this(
        headersOf(
            MESSAGE to message,
            RECEIPT_ID to receiptId,
            customHeaders = customHeaders,
        )
    )
}

private fun acceptVersionHeader() = header(ACCEPT_VERSION) { it.split(',') }

private fun heartBeatHeader() = optionalHeader(HEART_BEAT) { it.toHeartBeat() }

private fun String.toHeartBeat(): HeartBeat {
    val (minSendPeriod, expectedReceivePeriod) = split(',')
    return HeartBeat(
        minSendPeriod = minSendPeriod.toInt().milliseconds,
        expectedPeriod = expectedReceivePeriod.toInt().milliseconds,
    )
}

private fun HeartBeat.formatAsHeaderValue() = "${minSendPeriod.inWholeMilliseconds},${expectedPeriod.inWholeMilliseconds}"
