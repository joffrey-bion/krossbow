package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.engine.*
import io.ktor.client.engine.curl.*

actual fun ktorEngine(): HttpClientEngineFactory<*> = Curl
