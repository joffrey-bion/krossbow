package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.request.*

actual class HttpGetter {
    private val client = HttpClient(Darwin)

    actual suspend fun get(url: String): String = client.get(url).body()
}
