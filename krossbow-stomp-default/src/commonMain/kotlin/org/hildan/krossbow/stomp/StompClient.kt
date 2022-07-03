package org.hildan.krossbow.stomp

import org.hildan.krossbow.stomp.config.StompConfig
import org.hildan.krossbow.websocket.WebSocketClient
import org.hildan.krossbow.websocket.builtin.builtIn

/**
 * Creates a new `StompClient` with a default web socket implementation using the built-in client from each platform.
 *
 * It uses the browser's WebSocket in JS environment, the JDK11's HttpClient on JVM, and the NSURLSession on Apple
 * targets.
 *
 * You can configure the client by passing an optional [configure] lambda.
 */
@Deprecated(
    message = "This helper loads a conflicting StompClientKt class and causes NoSuchMethodError upon connect(). " +
        "It will be removed in a future release. Please use an explicit web socket client instead.",
    replaceWith = ReplaceWith(
        expression = "StompClient(WebSocketClient.builtIn(), configure)",
        imports = ["org.hildan.krossbow.websocket.builtin.builtIn"],
    ),
    level = DeprecationLevel.ERROR,
)
fun StompClient(configure: StompConfig.() -> Unit = {}) = StompClient(
    webSocketClient = WebSocketClient.builtIn(),
    configure = configure,
)
