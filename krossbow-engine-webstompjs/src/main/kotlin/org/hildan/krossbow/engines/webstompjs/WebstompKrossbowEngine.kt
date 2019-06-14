package org.hildan.krossbow.engines.webstompjs

import js.webstomp.client.Client
import js.webstomp.client.Heartbeat
import js.webstomp.client.Options
import js.webstomp.client.client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hildan.krossbow.engines.HeartBeat
import org.hildan.krossbow.engines.KrossbowClient
import org.hildan.krossbow.engines.KrossbowConfig
import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.KrossbowEngineSession
import org.hildan.krossbow.engines.KrossbowEngineSubscription
import org.hildan.krossbow.engines.KrossbowReceipt
import org.hildan.krossbow.engines.KrossbowSession
import org.hildan.krossbow.engines.SubscriptionCallbacks
import kotlin.reflect.KClass

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
        return KrossbowSession(WebstompKrossbowSession(client))
    }
}

class WebstompKrossbowSession(private val client: Client): KrossbowEngineSession {

    override suspend fun send(destination: String, body: Any): KrossbowReceipt? = withContext(Dispatchers.Default) {
        client.send(destination, TODO("Serialize body to String"))
        null
    }

    override suspend fun <T : Any> subscribe(
        destination: String, clazz: KClass<T>, callbacks: SubscriptionCallbacks<T>
    ): KrossbowEngineSubscription {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun disconnect() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
