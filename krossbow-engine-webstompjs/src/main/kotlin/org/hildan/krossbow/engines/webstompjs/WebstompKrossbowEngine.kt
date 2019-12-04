package org.hildan.krossbow.engines.webstompjs

import Client
import ExtendedHeaders
import Heartbeat
import Message
import Options
import SockJS
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.hildan.krossbow.engines.ConnectionException
import org.hildan.krossbow.engines.HeartBeat
import org.hildan.krossbow.engines.InvalidFramePayloadException
import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.KrossbowEngineClient
import org.hildan.krossbow.engines.KrossbowEngineConfig
import org.hildan.krossbow.engines.KrossbowEngineSession
import org.hildan.krossbow.engines.KrossbowEngineSubscription
import org.hildan.krossbow.engines.KrossbowMessage
import org.hildan.krossbow.engines.KrossbowReceipt
import org.hildan.krossbow.engines.MessageHeaders
import org.hildan.krossbow.engines.SubscriptionCallbacks
import webstomp
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private class WebstompHeartbeat(
    override var outgoing: Number,
    override var incoming: Number
) : Heartbeat

private fun HeartBeat.toWebstomp(): WebstompHeartbeat = WebstompHeartbeat(minSendPeriodMillis, expectedPeriodMillis)

private class WebstompOptions(
    override var protocols: Array<String>? = emptyArray(),
    override var binary: Boolean? = false,
    override var heartbeat: WebstompHeartbeat,
    override var debug: Boolean? = false
) : Options

/**
 * Implementation of [KrossbowEngine] for JavaScript based on the `webstomp-client` NPM module.
 */
object WebstompKrossbowEngine : KrossbowEngine {

    override fun createClient(config: KrossbowEngineConfig): KrossbowEngineClient {
        if (config.autoReceipt) {
            throw IllegalArgumentException("Receipts not supported yet by JS client")
        }
        return WebstompKrossbowClient(config)
    }
}

private class WebstompKrossbowClient(private val config: KrossbowEngineConfig) : KrossbowEngineClient {

    override suspend fun connect(url: String, login: String?, passcode: String?): KrossbowEngineSession {
        val options = WebstompOptions(heartbeat = config.heartBeat.toWebstomp())
        val client = webstomp.over(SockJS(url), options)
        return suspendCoroutine { cont ->
            // TODO headers
            client.connect(js("{}"), {
                cont.resume(WebstompKrossbowSession(client))
            }, { err ->
                cont.resumeWithException(ConnectionException("webstomp connect failed with the following error: ${JSON.stringify(err)}"))
            })
        }
    }
}

private class WebstompKrossbowSession(private val client: Client) : KrossbowEngineSession {

    @UseExperimental(ExperimentalStdlibApi::class)
    override suspend fun send(destination: String, body: ByteArray?): KrossbowReceipt? {
        client.send(destination, body?.let { body.decodeToString() })
        return null
    }

    @UseExperimental(ExperimentalStdlibApi::class)
    override suspend fun subscribe(
        destination: String,
        callbacks: SubscriptionCallbacks<ByteArray>
    ): KrossbowEngineSubscription = subscribe(destination, callbacks) { body ->
        when (body) {
            null -> throw InvalidFramePayloadException("A payload was expected but nothing was received")
            else -> body.encodeToByteArray()
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

    private suspend inline fun <T : Any> subscribe(
        destination: String,
        callbacks: SubscriptionCallbacks<T>,
        crossinline convertPayload: (String?) -> T
    ): KrossbowEngineSubscription {
        console.log("Subscribing to $destination")
        val sub = client.subscribe(destination, { m: Message ->
            try {
                val msg = KrossbowMessage(convertPayload(m.body), m.headers.toKrossbowHeaders())
                GlobalScope.promise { callbacks.onReceive(msg) }
            } catch (e: Exception) {
                callbacks.onError(e)
            }
        })
        return KrossbowEngineSubscription(sub.id) { sub.unsubscribe() }
    }

    override suspend fun disconnect(): Unit = suspendCoroutine { cont ->
        client.disconnect({ cont.resume(Unit) }, js("{}")) // TODO headers
    }
}

private fun ExtendedHeaders.toKrossbowHeaders(): MessageHeaders = object : MessageHeaders {} // TODO fill that up
