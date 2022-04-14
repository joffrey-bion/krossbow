package org.hildan.krossbow.websocket.test.autobahn

import java.net.*

actual class HttpGetter {
    actual suspend fun get(url: String): String = URL(url).readText()
}
