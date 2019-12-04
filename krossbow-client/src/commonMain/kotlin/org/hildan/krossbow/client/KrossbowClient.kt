package org.hildan.krossbow.client

import org.hildan.krossbow.engines.KrossbowEngine

internal expect fun defaultEngine(): KrossbowEngine

/**
 * A STOMP client based on web sockets. The client is used to connect to the server and create a [KrossbowSession].
 * Then, most of the STOMP interactions are done through the [KrossbowSession].
 */
class KrossbowClient(engine: KrossbowEngine = defaultEngine(), configure: KrossbowConfig.() -> Unit = {}) {

    private val config = KrossbowConfig().apply { configure() }

    private val engineClient = engine.createClient(config)

    /**
     * Connects to the given WebSocket [url] and to the STOMP session, and returns after receiving the CONNECTED frame.
     */
    suspend fun connect(url: String, login: String? = null, passcode: String? = null): KrossbowSession =
            KrossbowSession(engineClient.connect(url, login, passcode), config)
}

/**
 * Connects to the given [url] and executes the given [block] with the created session. The session is then
 * automatically closed at the end of the block.
 */
suspend fun KrossbowClient.useSession(
    url: String,
    login: String? = null,
    passcode: String? = null,
    block: suspend KrossbowSession.() -> Unit
) {
    val session = connect(url, login, passcode)
    try {
        session.block()
    } finally {
        session.disconnect()
    }
}
