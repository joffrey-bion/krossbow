package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*

internal actual fun ktorEngine(): HttpClientEngineFactory<*> = CIO
