package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*

actual fun ktorEngine(): HttpClientEngineFactory<*> = Js
