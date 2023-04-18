package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.request.*

actual class HttpGetter {
    private val client = HttpClient(ktorEngine())

    actual suspend fun get(url: String): String = client.get(url).body()
}

expect fun ktorEngine(): HttpClientEngineFactory<*>
