package org.hildan.krossbow.websocket.test.autobahn

import io.ktor.client.engine.*
import io.ktor.client.engine.java.*

actual fun ktorEngine(): HttpClientEngineFactory<*> = Java
