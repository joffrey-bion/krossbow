package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

actual fun ktorEngine(): HttpClientEngineFactory<*> = Darwin
