package org.hildan.krossbow.engines.mpp

import org.hildan.krossbow.engines.KrossbowEngine
import org.hildan.krossbow.engines.KrossbowEngineClient
import org.hildan.krossbow.engines.KrossbowEngineConfig
import org.hildan.krossbow.engines.KrossbowEngineSession
import org.hildan.krossbow.engines.KrossbowEngineSubscription
import org.hildan.krossbow.engines.KrossbowReceipt
import org.hildan.krossbow.engines.SubscriptionCallbacks

object MppKrossbowEngine: KrossbowEngine {

    override fun createClient(config: KrossbowEngineConfig): KrossbowEngineClient = MppKrossbowEngineClient(config)
}

class MppKrossbowEngineClient(
    val config: KrossbowEngineConfig
): KrossbowEngineClient {

    override suspend fun connect(url: String, login: String?, passcode: String?): KrossbowEngineSession =
            MppKrossbowEngineSession()
}

/**
 * An engine-specific STOMP session.
 */
class MppKrossbowEngineSession: KrossbowEngineSession {

    override suspend fun send(destination: String, body: ByteArray?): KrossbowReceipt? {
        TODO()
    }

    override suspend fun subscribe(destination: String, callbacks: SubscriptionCallbacks<ByteArray>): KrossbowEngineSubscription {
        TODO()
    }

    override suspend fun subscribeNoPayload(destination: String, callbacks: SubscriptionCallbacks<Unit>): KrossbowEngineSubscription {
        TODO()
    }

    override suspend fun disconnect() {
        TODO()
    }
}
