# Krossbow Web Socket Ktor

This module provides the `KtorWebSocketClient`, a multiplatform implementation of the general Web Socket interface
defined by `krossbow-websocket-core`, based on [Ktor's Web Socket Client](https://ktor.io/clients/websockets.html).

Ktor uses [pluggable engines](https://ktor.io/clients/http-client/engines.html) to perform the platform-specific 
network operations (just like Krossbow uses different web socket implementations).
You need to pick an engine that supports web sockets in order to use Ktor's HttpClient with web sockets.
Follow Ktor's documentation to find out more about how to use engines.

## Dependency

You will need to declare the `krossbow-websocket-ktor` module dependency to use the `KtorWebSocketClient`:

```
implementation("org.hildan.krossbow:krossbow-websocket-ktor:$krossbowVersion")
```

## Usage with StompClient

To use the `KtorWebSocketClient` instead of the platform default,
you need to specify it when creating your `StompClient`:

```kotlin
val client = StompClient(KtorWebSocketClient())
```

You can customize the actual HTTP client used by Ktor behind the scenes by passing it to `KtorWebSocketClient`:

```kotlin
// You may configure Ktor HTTP client as you please,
// but make sure at least the websocket feature is installed
val httpClient = HttpClient {
    install(WebSockets)
}
val wsClient = KtorWebSocketClient(httpClient)
val stompClient = StompClient(wsClient)
```