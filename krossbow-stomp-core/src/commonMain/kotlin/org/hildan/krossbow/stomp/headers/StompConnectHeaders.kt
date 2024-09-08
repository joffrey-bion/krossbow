package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.config.*
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.ACCEPT_VERSION
import org.hildan.krossbow.stomp.headers.HeaderNames.HEART_BEAT
import org.hildan.krossbow.stomp.headers.HeaderNames.HOST
import org.hildan.krossbow.stomp.headers.HeaderNames.LOGIN
import org.hildan.krossbow.stomp.headers.HeaderNames.PASSCODE
import org.hildan.krossbow.stomp.version.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * The headers of a [StompFrame.Connect] frame.
 */
interface StompConnectHeaders : StompHeaders {
    /**
     * The name of the host server to connect to.
     */
    val host: String? // mandatory since 1.1, but forbidden in some 1.0 servers
    /**
     * A list of versions of the protocol supported by this client.
     * By default, Krossbow sets what it supports, but this can be made more strict if desired.
     */
    val acceptVersion: List<String>
    /**
     * An optional login to connect to the server.
     */
    val login: String?
    /**
     * An optional passcode to connect to the server.
     */
    val passcode: String?
    /**
     * The heart-beating settings desired by the client.
     */
    val heartBeat: HeartBeat?
}

/**
 * A temporary mutable representation of [StompConnectHeaders] to ease their construction (or copy with modification).
 */
interface StompConnectHeadersBuilder : StompConnectHeaders, StompHeadersBuilder {
    override var host: String?
    override var acceptVersion: List<String>
    override var login: String?
    override var passcode: String?
    override var heartBeat: HeartBeat?
}

/**
 * Creates an instance of [StompConnectHeaders] with the given [host] header.
 * Optional headers can be configured using the [configure] lambda.
 */
// We put a 'host' parameter without a default value because it is required since STOMP 1.1.
// Setting 'null' should be a conscious choice and not a default.
fun StompConnectHeaders(host: String?, configure: StompConnectHeadersBuilder.() -> Unit = {}): StompConnectHeaders =
    MapBasedStompConnectHeaders().apply {
        this.host = host
        configure()
    }

@Deprecated(
    message = "This overload will be removed in a future version, please use the overload with lambda instead to set optional headers.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
        expression = "StompConnectHeaders(host) {\n" +
            "    this.acceptVersion = acceptVersion\n" +
            "    this.login = login\n" +
            "    this.passcode = passcode\n" +
            "    this.heartBeat = heartBeat\n" +
            "    putAll(customHeaders)\n" +
            "}",
        imports = [ "org.hildan.krossbow.stomp.headers.StompConnectHeaders" ],
    ),
)
fun StompConnectHeaders(
    host: String?,
    acceptVersion: List<String> = StompVersion.preferredOrder.map { it.headerValue },
    login: String? = null,
    passcode: String? = null,
    heartBeat: HeartBeat? = null,
    customHeaders: Map<String, String> = emptyMap(),
): StompConnectHeaders = StompConnectHeaders(host) {
    this.acceptVersion = acceptVersion
    this.login = login
    this.passcode = passcode
    this.heartBeat = heartBeat
    setAll(customHeaders)
}

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompConnectHeaders.copy(transform: StompConnectHeadersBuilder.() -> Unit = {}): StompConnectHeaders =
    MapBasedStompConnectHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompConnectHeaders(rawHeaders: MutableMap<String, String>): StompConnectHeaders =
    MapBasedStompConnectHeaders(backingMap = rawHeaders)

private class MapBasedStompConnectHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompConnectHeadersBuilder {

    override var host: String? by optionalHeader(HOST) // required since 1.1, but forbidden in some 1.0 servers
    override var acceptVersion: List<String> by requiredHeader(
        name = ACCEPT_VERSION,
        preset = StompVersion.preferredOrder.map { it.headerValue },
        decode = { it.split(',') },
        encode = { it.joinToString(",") },
    )
    override var login: String? by optionalHeader(LOGIN)
    override var passcode: String? by optionalHeader(PASSCODE)
    override var heartBeat: HeartBeat? by heartBeatHeader()
}

internal fun heartBeatHeader() = optionalHeader(
    name = HEART_BEAT,
    default = null,
    decode = { it.toHeartBeat() },
    encode = { it?.formatAsHeaderValue() },
)

private fun String.toHeartBeat(): HeartBeat {
    val (minSendPeriod, expectedReceivePeriod) = split(',')
    return HeartBeat(
        minSendPeriod = minSendPeriod.toInt().milliseconds,
        expectedPeriod = expectedReceivePeriod.toInt().milliseconds,
    )
}

private fun HeartBeat.formatAsHeaderValue() = "${minSendPeriod.inWholeMilliseconds},${expectedPeriod.inWholeMilliseconds}"
