package org.hildan.krossbow.engines.mpp

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.KrossbowEngineClient
import org.hildan.krossbow.engines.KrossbowEngineConfig
import org.hildan.krossbow.engines.KrossbowEngineSession
import org.hildan.krossbow.engines.KrossbowEngineSubscription
import org.hildan.krossbow.engines.KrossbowMessage
import org.hildan.krossbow.engines.KrossbowReceipt
import org.hildan.krossbow.engines.MessageHeaders
import org.hildan.krossbow.engines.SubscriptionCallbacks
import org.hildan.krossbow.engines.mpp.frame.FrameBody
import org.hildan.krossbow.engines.mpp.frame.StompCommand
import org.hildan.krossbow.engines.mpp.frame.StompFrame
import org.hildan.krossbow.engines.mpp.frame.StompParser
import org.hildan.krossbow.engines.mpp.headers.StompConnectHeaders
import org.hildan.krossbow.engines.mpp.headers.StompDisconnectHeaders
import org.hildan.krossbow.engines.mpp.headers.StompSendHeaders
import org.hildan.krossbow.engines.mpp.headers.StompSubscribeHeaders
import org.hildan.krossbow.engines.mpp.headers.StompUnsubscribeHeaders
import org.hildan.krossbow.engines.mpp.websocket.KtorWebSocket
import org.hildan.krossbow.engines.mpp.websocket.WebSocket
import org.hildan.krossbow.engines.mpp.websocket.WebSocketSession

object MppKrossbowEngine: KrossbowEngine {

    override fun createClient(config: KrossbowEngineConfig): KrossbowEngineClient =
            MppKrossbowEngineClient(config, KtorWebSocket())
}

@UseExperimental(KtorExperimentalAPI::class)
class MppKrossbowEngineClient(
    private val config: KrossbowEngineConfig,
    private val webSocket: WebSocket
): KrossbowEngineClient {

    override suspend fun connect(url: String, login: String?, passcode: String?): KrossbowEngineSession =
            MppKrossbowEngineSession(config, webSocket.connect(url)).apply {
                // connect at STOMP protocol level
                connect(url, login, passcode)
            }
}

class MppKrossbowEngineSession(
    private val config: KrossbowEngineConfig,
    private val webSocketSession: WebSocketSession
): KrossbowEngineSession {

    private var nextSubscriptionId = 0

    private val subscriptions: MutableMap<String, SubscriptionCallbacks<ByteArray>> = mutableMapOf()

    private val frameChannels = StompCommand.values().associate { it to Channel<StompFrame>(1) }

    @UseExperimental(ExperimentalStdlibApi::class)
    private val wsListenerJob = GlobalScope.launch {
        for (frameBytes in webSocketSession.incomingFrames) {
            when (val frame = StompParser.parse(frameBytes)) {
                is StompFrame.Message -> onMessageFrameReceived(frame)
                else -> frameChannels.getValue(frame.command).send(frame)
            }
        }
    }

    private suspend fun onMessageFrameReceived(frame: StompFrame.Message) {
        val payload = frame.body?.rawBytes ?: ByteArray(0)
        val headers = object : MessageHeaders {} // TODO use actual headers
        subscriptions.getValue(frame.headers.subscription).onReceive(KrossbowMessage(payload, headers))
    }

    suspend fun connect(url: String, login: String?, passcode: String?) {
        val host = url.substringAfter("://").substringBefore("/").substringBefore(":")
        sendStompFrame(StompFrame.Connect(StompConnectHeaders(host = host, login = login, passcode = passcode)))
        val connectedFrame = waitForFrame<StompFrame.Connected>(StompCommand.CONNECTED)
        // TODO use connected frame to setup heartbeat config
    }

    private suspend inline fun <reified T : StompFrame> waitForFrame(command: StompCommand): T =
            frameChannels.getValue(command).receive() as T

    @UseExperimental(ExperimentalStdlibApi::class)
    override suspend fun send(destination: String, body: ByteArray?): KrossbowReceipt? {
        sendStompFrame(StompFrame.Send(StompSendHeaders(destination), body?.let { FrameBody.Binary(it) }))
        // TODO suspend until receipt
        return null
    }

    override suspend fun subscribe(destination: String, callbacks: SubscriptionCallbacks<ByteArray>): KrossbowEngineSubscription {
        val id = (nextSubscriptionId++).toString()
        subscriptions[id] = callbacks
        sendStompFrame(StompFrame.Subscribe(StompSubscribeHeaders(destination = destination, id = id)))
        // TODO suspend until receipt
        return KrossbowEngineSubscription(id) {
            sendStompFrame(StompFrame.Unsubscribe(StompUnsubscribeHeaders(id = id)))
            subscriptions.remove(id)
        }
    }

    override suspend fun subscribeNoPayload(destination: String, callbacks: SubscriptionCallbacks<Unit>): KrossbowEngineSubscription {
        return subscribe(destination, object : SubscriptionCallbacks<ByteArray> {
            override suspend fun onReceive(message: KrossbowMessage<ByteArray>) =
                    callbacks.onReceive(KrossbowMessage(Unit, message.headers))

            override fun onError(throwable: Throwable) = callbacks.onError(throwable)
        })
    }

    private suspend fun sendStompFrame(frame: StompFrame) {
        webSocketSession.send(frame.toBytes())
    }

    override suspend fun disconnect() {
        sendStompFrame(StompFrame.Disconnect(StompDisconnectHeaders()))
        wsListenerJob.cancelAndJoin()
        webSocketSession.close()
    }
}
