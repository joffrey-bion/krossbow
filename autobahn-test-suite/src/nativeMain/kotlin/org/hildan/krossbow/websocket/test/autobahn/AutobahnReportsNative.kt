package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.request.*

internal actual fun HttpGetter(): HttpGetter = KtorHttpGetter()

private class KtorHttpGetter : HttpGetter {
    private val client = HttpClient(ktorEngine())

    override suspend fun get(url: String): String = client.get(url).body()
}

internal expect fun ktorEngine(): HttpClientEngineFactory<*>
