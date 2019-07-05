package org.hildan.krossbow.engines.webstompjs

import js.webstomp.client.Client
import js.webstomp.client.ExtendedHeaders
import js.webstomp.client.Heartbeat
import js.webstomp.client.Message
import js.webstomp.client.Options
import js.webstomp.client.client
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.hildan.krossbow.engines.HeartBeat
import org.hildan.krossbow.engines.KrossbowClient
import org.hildan.krossbow.engines.KrossbowConfig
import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.KrossbowEngineSession
import org.hildan.krossbow.engines.KrossbowEngineSubscription
import org.hildan.krossbow.engines.KrossbowMessage
import org.hildan.krossbow.engines.KrossbowReceipt
import org.hildan.krossbow.engines.KrossbowSession
import org.hildan.krossbow.engines.MessageHeaders
import org.hildan.krossbow.engines.SubscriptionCallbacks
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
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

object WebstompKrossbowEngine : KrossbowEngine {

    override fun createClient(config: KrossbowConfig): KrossbowClient {
        if (config.autoReceipt) {
            throw IllegalArgumentException("Receipts not supported yet by JS client")
        }
        return WebstompKrossbowClient(config)
    }
}

class WebstompKrossbowClient(private val config: KrossbowConfig) : KrossbowClient {

    override suspend fun connect(url: String, login: String?, passcode: String?): KrossbowSession {
        val options = WebstompOptions(heartbeat = config.heartBeat.toWebstomp())
        val client = client(url, options)
        return KrossbowSession(WebstompKrossbowSession(client))
    }
}

class WebstompKrossbowSession(private val client: Client) : KrossbowEngineSession {

    override suspend fun send(destination: String, body: Any?): KrossbowReceipt? {
        client.send(destination, body?.let { JSON.stringify(it) })
        return null
    }

    override suspend fun <T : Any> subscribe(
        destination: String,
        clazz: KClass<T>,
        callbacks: SubscriptionCallbacks<T>
    ): KrossbowEngineSubscription {

        val sub = client.subscribe(destination, { m: Message ->
            val payload = JSON.parse<T>(m.body)
            val msg = KrossbowMessage(payload, m.headers.toKrossbowHeaders())
            GlobalScope.promise { callbacks.onReceive(msg) }
        })
        return KrossbowEngineSubscription(sub.id) { sub.unsubscribe() }
    }

    override suspend fun disconnect(): Unit = suspendCoroutine { cont ->
        client.disconnect({ cont.resume(Unit) }, null) // TODO headers
    }
}

private fun ExtendedHeaders.toKrossbowHeaders(): MessageHeaders = object : MessageHeaders {} // TODO fill that up
