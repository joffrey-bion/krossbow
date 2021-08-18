package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.engine.*
import io.ktor.client.engine.ios.*

actual fun ktorEngine(): HttpClientEngineFactory<*> = Ios
