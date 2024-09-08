package org.hildan.krossbow.stomp.headers

import org.hildan.krossbow.stomp.config.*
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.HeaderNames.SERVER
import org.hildan.krossbow.stomp.headers.HeaderNames.SESSION
import org.hildan.krossbow.stomp.headers.HeaderNames.VERSION

/**
 * The headers of a [StompFrame.Connected] frame.
 */
interface StompConnectedHeaders : StompHeaders {
    /**
     * The version of the STOMP protocol the session will be using.
     */
    val version: String // mandatory since 1.1, but not sent by 1.0 servers
    /**
     * A session identifier that uniquely identifies the session.
     */
    val session: String?
    /**
     * Information about the STOMP server.
     */
    val server: ServerInfo?
    /**
     * The heart-beating settings desired by the server (the effective heartbeats are the result of a negotiation).
     */
    val heartBeat: HeartBeat?
}

/**
 * A temporary mutable representation of [StompConnectedHeaders] to ease their construction (or copy with modification).
 */
interface StompConnectedHeadersBuilder : StompConnectedHeaders, StompHeadersBuilder {
    override var version: String
    override var session: String?
    override var server: ServerInfo?
    override var heartBeat: HeartBeat?
}

/**
 * Creates an instance of [StompConnectedHeaders].
 * All headers are optional and can be configured using the [configure] lambda.
 */
fun StompConnectedHeaders(configure: StompConnectedHeadersBuilder.() -> Unit = {}): StompConnectedHeaders =
    MapBasedStompConnectedHeaders().apply(configure)

@Deprecated(
    message = "This overload will be removed in a future version, please use the overload with lambda instead to set optional headers.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith(
        expression = "StompConnectedHeaders {\n" +
            "    this.version = version\n" +
            "    this.session = session\n" +
            "    this.server = server\n" +
            "    this.heartBeat = heartBeat\n" +
            "}",
        imports = [ "org.hildan.krossbow.stomp.headers.StompConnectedHeaders" ],
    ),
)
fun StompConnectedHeaders(
    version: String = "1.2",
    session: String? = null,
    server: String? = null,
    heartBeat: HeartBeat? = null,
): StompConnectedHeaders = StompConnectedHeaders {
    this.version = version
    this.session = session
    this.server = server?.let { ServerInfo.fromHeader(it) }
    this.heartBeat = heartBeat
}

/**
 * Creates a copy of these headers with the given [transform] applied.
 */
fun StompConnectedHeaders.copy(transform: StompConnectedHeadersBuilder.() -> Unit = {}): StompConnectedHeaders =
    MapBasedStompConnectedHeaders(backingMap = asMap().toMutableMap()).apply(transform)

internal fun StompConnectedHeaders(rawHeaders: MutableMap<String, String>): StompConnectedHeaders {
    if (VERSION !in rawHeaders) {
        // The version header is mandatory since 1.1, but is not sent by 1.0 servers.
        // If we receive a frame without it, we should consider it from such a server.
        rawHeaders[VERSION] = "1.0"
    }
    return StompConnectedHeaders(rawHeaders)
}

private class MapBasedStompConnectedHeaders(
    backingMap: MutableMap<String, String> = mutableMapOf(),
) : MapBasedStompHeaders(backingMap), StompConnectedHeadersBuilder {
    override var version: String by requiredHeader(VERSION, "1.2")
    override var session: String? by optionalHeader(SESSION)
    override var server: ServerInfo? by optionalHeader(
        name = SERVER,
        default = null,
        encode = { it?.formatAsHeaderValue() },
        decode = { ServerInfo.fromHeader(it) },
    )
    override var heartBeat: HeartBeat? by heartBeatHeader()
}

/**
 * Represents information about the STOMP server.
 */
data class ServerInfo(
    /**
     * The name of the server vendor or implementation.
     */
    val name: String,
    /**
     * The version of the server itself (not of the STOMP protocol that it speaks).
     */
    val version: String?,
    /**
     * Optional comments.
     */
    val comments: List<String> = emptyList(),
) {
    companion object {
        private val serverInfoRegex = Regex("""(?<name>[^/\s]+)(?:/(?<version>\S+))?(?: (?<comments>.*))?""")

        internal fun fromHeader(value: String): ServerInfo {
            val match = serverInfoRegex.matchEntire(value)
                ?: throw InvalidServerHeaderException("Invalid 'server' header value '$value', expected format '${serverInfoRegex.pattern}'", value)
            return ServerInfo(
                name = match.groups["name"]?.value ?: error(value),
                version = match.groups["version"]?.value,
                comments = match.groups["comments"]?.value?.split(' ') ?: emptyList(),
            )
        }
    }
}

private fun ServerInfo.formatAsHeaderValue(): String = buildString {
    append(name)
    if (version != null) {
        append("/$version")
    }
    if (comments.isNotEmpty()) {
        append(" ${comments.joinToString(" ")}")
    }
}

/**
 * An exception thrown when an invalid value is found in a `server` header.
 * Such values must be of the form `name ["/" version] *(comment)`.
 */
class InvalidServerHeaderException(message: String, val invalidText: String) : Exception(message)
