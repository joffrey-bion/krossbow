package org.hildan.krossbow.client.headers

import org.hildan.krossbow.client.frame.HeartBeat
import org.hildan.krossbow.client.frame.formatAsHeaderValue
import org.hildan.krossbow.client.headers.HeaderKeys.ACCEPT_VERSION
import org.hildan.krossbow.client.headers.HeaderKeys.DESTINATION
import org.hildan.krossbow.client.headers.HeaderKeys.HEART_BEAT
import org.hildan.krossbow.client.headers.HeaderKeys.HOST
import org.hildan.krossbow.client.headers.HeaderKeys.LOGIN
import org.hildan.krossbow.client.headers.HeaderKeys.PASSCODE
import org.hildan.krossbow.client.headers.HeaderKeys.RECEIPT
import org.hildan.krossbow.client.headers.HeaderKeys.SERVER
import org.hildan.krossbow.client.headers.HeaderKeys.SESSION
import org.hildan.krossbow.client.headers.HeaderKeys.TRANSACTION
import org.hildan.krossbow.client.headers.HeaderKeys.VERSION

object HeaderKeys {
    const val ACCEPT_VERSION = "accept-version"
    const val CONTENT_LENGTH = "content-length"
    const val DESTINATION = "destination"
    const val HEART_BEAT = "heart-beat"
    const val HOST = "host"
    const val LOGIN = "login"
    const val PASSCODE = "passcode"
    const val RECEIPT = "receipt"
    const val SERVER = "server"
    const val SESSION = "session"
    const val TRANSACTION = "transaction"
    const val VERSION = "version"
}

data class StompHeader(
    val key: String,
    val value: String,
    val formerValues: List<String> = listOf()
)

interface StompHeaders {
    /**
     * Gets the header with the given key, or null if the header is absent.
     */
    operator fun get(key: String): StompHeader?
    /**
     * Gets the current value of the given header, or null if the header is absent.
     */
    fun getValue(key: String): String? = get(key)?.value
    /**
     * Gets all headers from this container.
     */
    fun getAll(): Collection<StompHeader>
}

internal data class SimpleStompHeaders(
    private val headers: Map<String, StompHeader>
): StompHeaders {
    override fun get(key: String): StompHeader? = headers[key]
    override fun getAll(): Collection<StompHeader> = headers.values
}

fun Map<String, StompHeader>.toHeaders(): StompHeaders = SimpleStompHeaders(this)

private fun Pair<String, String?>.toHeader(): StompHeader? = second?.let { StompHeader(first, it) }

fun headersOf(
    vararg pairs: Pair<String, String?>,
    customHeaders: Map<String, String> = emptyMap()
): StompHeaders {
    val headerPairs = pairs.asList() + customHeaders.toList()
    return headerPairs.mapNotNull { it.toHeader() }.associateBy { it.key }.toHeaders()
}

interface StandardHeaders: StompHeaders {
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
            VERSION to version,
            SESSION to session,
            SERVER to server,
            HEART_BEAT to heartBeat?.formatAsHeaderValue()
        )
    )
}

class StompDisconnectHeaders(rawHeaders: StompHeaders) : StompHeaders by rawHeaders {
    val receipt: String? by optionalHeader()

    constructor(receipt: String? = null) : this(headersOf(RECEIPT to receipt))
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
