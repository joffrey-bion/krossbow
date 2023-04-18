package org.hildan.krossbow.websocket.test.autobahn

import fetch
import kotlinx.coroutines.await

actual class HttpGetter {

    actual suspend fun get(url: String): String = fetch(url).await().text().await()
}
