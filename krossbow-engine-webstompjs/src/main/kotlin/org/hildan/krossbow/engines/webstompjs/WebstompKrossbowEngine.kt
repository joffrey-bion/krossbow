package org.hildan.krossbow.engines.webstompjs

import js.webstomp.client.Client
import js.webstomp.client.Heartbeat
import js.webstomp.client.client
import js.webstomp.client.Options
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import org.hildan.krossbow.engines.HeartBeat
import org.hildan.krossbow.engines.KrossbowClient
import org.hildan.krossbow.engines.KrossbowConfig
import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.KrossbowSession

class WebstompHeartbeat(
    override var outgoing: Number,
    override var incoming: Number
) : Heartbeat

private fun HeartBeat.toWebstomp(): WebstompHeartbeat = WebstompHeartbeat(minSendPeriodMillis, expectedPeriodMillis)

class WebstompOptions(
    override var protocols: Array<String>? = null,
    override var binary: Boolean = false,
    override var heartbeat: WebstompHeartbeat,
    override var debug: Boolean = false
) : Options

object WebstompKrossbowEngine: KrossbowEngine {

    override fun createClient(config: KrossbowConfig): KrossbowClient = WebstompKrossbowClient(config)
}

class WebstompKrossbowClient(private val config: KrossbowConfig): KrossbowClient {

    override suspend fun connect(url: String, login: String?, passcode: String?): KrossbowSession {
        val options = WebstompOptions(heartbeat = config.heartBeat.toWebstomp())
        val client = client(url, options)
        return WebstompKrossbowSession(client)
    }
}

class WebstompKrossbowSession(private val client: Client): KrossbowSession {

    override suspend fun send(destination: String, body: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun <T> subscribe(destination: String, onFrameReceived: (T) -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun <T> CoroutineScope.subscribe(destination: String): ReceiveChannel<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun disconnect() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
