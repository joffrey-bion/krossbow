package org.hildan.krossbow.stomp

import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.default

/**
 * Creates a new `StompClient` with a default web socket implementation using the built-in client from each platform.
 *
 * It uses the browser's WebSocket in JS environment, the JDK11's HttpClient on JVM, and the NSURLSession on Apple
 * targets.
 *
 * You can configure the client by passing an optional [configure] lambda.
 */
fun StompClient(configure: StompConfig.() -> Unit = {}) = StompClient(
    webSocketClient = WebSocketClient.default(),
    config = StompConfig().apply { configure() },
)
