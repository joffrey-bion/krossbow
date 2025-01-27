# Krossbow with Ktor

Krossbow allows you to use [Ktor's web socket](https://ktor.io/docs/client-websockets.html) as transport for STOMP.

Ktor's implementation supports a variety of platforms and is very popular in the Kotlin world, especially in Kotlin multiplatform.

The `krossbow-websocket-ktor` module provides the `KtorWebSocketClient`, which adapts Ktor {{ versions.ktor }}'s
`HttpClient` to Krossbow's web socket interface.

!!! info "Stuck with Ktor 2?"
    Krossbow updated to Ktor 3 to benefit from all the performance improvements and the new WASM platform support.
    If you are stuck with Ktor 2 for some reason, use the `krossbow-websocket-ktor-legacy` artifact instead.
    You can then add Ktor dependencies in version {{ versions.ktorLegacy }}.

## Usage with StompClient

To use the `KtorWebSocketClient` pass an instance of it when creating your `StompClient`:

```kotlin
val client = StompClient(KtorWebSocketClient())
```

You can customize the actual Ktor HTTP client used behind the scenes by passing it to `KtorWebSocketClient`:

```kotlin
// You may configure Ktor HTTP client as you please,
// but make sure at least the websocket feature is installed
val httpClient = HttpClient {
    install(WebSockets)
}
val wsClient = KtorWebSocketClient(httpClient)
val stompClient = StompClient(wsClient)
```

## Dependency information

You will need to declare the following Gradle dependency to use the `KtorWebSocketClient`:

```kotlin
implementation("org.hildan.krossbow:krossbow-websocket-ktor:{{ git.short_tag }}")
```

Ktor uses [pluggable engines](https://ktor.io/docs/client-engines.html) to perform the platform-specific
network operations (just like Krossbow uses different web socket implementations).
You need to pick an engine that supports web sockets in order to use Ktor's `HttpClient` with web sockets.
Follow Ktor's documentation to find out more about how to use engines.

For instance, if you want to use Ktor's CIO engine with Krossbow, you need to declare the following:

```kotlin
implementation("org.hildan.krossbow:krossbow-websocket-ktor:{{ git.short_tag }}")
implementation("io.ktor:ktor-client-cio:{{ versions.ktor }}")
```
