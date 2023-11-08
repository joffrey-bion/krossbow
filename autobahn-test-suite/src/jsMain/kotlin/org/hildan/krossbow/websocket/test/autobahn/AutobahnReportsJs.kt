package org.hildan.krossbow.websocket.test.autobahn

import fetch
import kotlinx.coroutines.await

internal actual fun HttpGetter() = HttpGetter { url -> fetch(url).await().text().await() }
