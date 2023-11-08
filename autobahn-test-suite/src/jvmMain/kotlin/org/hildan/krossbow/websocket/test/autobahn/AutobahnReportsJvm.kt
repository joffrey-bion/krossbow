package org.hildan.krossbow.websocket.test.autobahn

import java.net.*

internal actual fun HttpGetter() = HttpGetter { url -> URL(url).readText() }
