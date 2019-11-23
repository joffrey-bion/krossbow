package org.hildan.krossbow.engines.webstompjs

import Client
import ExtendedHeaders
import Heartbeat
import Message
import Options
import webstomp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.hildan.krossbow.engines.HeartBeat
import org.hildan.krossbow.engines.InvalidFramePayloadException
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
    override var protocols: Array<String>? = emptyArray(),
    override var binary: Boolean? = false,
    override var heartbeat: WebstompHeartbeat,
    override var debug: Boolean? = false
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
        val client = webstomp.client(url, options)
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
    ): KrossbowEngineSubscription = subscribe(destination, callbacks) { body ->
        when (body) {
            null -> throw InvalidFramePayloadException("Unsupported null websocket payload, expected $clazz")
            else -> JSON.parse<T>(body)
        }
    }

    override suspend fun subscribeNoPayload(
        destination: String,
        callbacks: SubscriptionCallbacks<Unit>
    ): KrossbowEngineSubscription = subscribe(destination, callbacks) { body ->
        when (body) {
            null, "" -> Unit
            else -> throw InvalidFramePayloadException("No payload was expected but some content was received")
        }
    }

    private fun <T : Any> subscribe(
        destination: String,
        callbacks: SubscriptionCallbacks<T>,
        convertPayload: (String?) -> T
    ): KrossbowEngineSubscription {
        val sub = client.subscribe(destination, { m: Message ->
            val msg = KrossbowMessage(convertPayload(m.body), m.headers.toKrossbowHeaders())
            GlobalScope.promise { callbacks.onReceive(msg) }
        })
        return KrossbowEngineSubscription(sub.id) { sub.unsubscribe() }
    }

    override suspend fun disconnect(): Unit = suspendCoroutine { cont ->
        client.disconnect({ cont.resume(Unit) }, js("{}")) // TODO headers
    }
}

private fun ExtendedHeaders.toKrossbowHeaders(): MessageHeaders = object : MessageHeaders {} // TODO fill that up
