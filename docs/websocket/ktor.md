# Krossbow with Ktor

Krossbow allows you to use [Ktor's web socket](https://ktor.io/clients/websockets.html) as transport for STOMP.

Ktor's implementation supports a variety of platforms and is very popular in the Kotlin world, especially in Kotlin multiplatform.

The `krossbow-websocket-ktor` module provides the `KtorWebSocketClient`, which adapts Ktor {{ versions.ktor }}'s
`HttpClient` to Krossbow's web socket interface.

!!! info "Still stuck with Ktor 1.6?"
    Not all projects can easily migrate to Ktor 2, that's why Krossbow provides the `krossbow-websocket-ktor-legacy`.
    Use that module instead of `krossbow-websocket-ktor` if you want to use Ktor {{ versions.ktorLegacy }}'s `HttpClient`.

## Usage with StompClient

To use the `KtorWebSocketClient` instead of the platform default,
you need to specify it when creating your `StompClient`:

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
implementation("org.hildan.krossbow:krossbow-websocket-ktor:{{ git.tag }}")
```

Ktor uses [pluggable engines](https://ktor.io/clients/http-client/engines.html) to perform the platform-specific
network operations (just like Krossbow uses different web socket implementations).
You need to pick an engine that supports web sockets in order to use Ktor's `HttpClient` with web sockets.
Follow Ktor's documentation to find out more about how to use engines.

For instance, if you want to use Ktor's CIO engine with Krossbow, you need to declare the following:

```kotlin
implementation("org.hildan.krossbow:krossbow-websocket-ktor:{{ git.tag }}")
implementation("io.ktor:ktor-client-cio:{{ versions.ktor }}")
```

If you want to use the older version 1.6.x of Ktor, use `krossbow-websocket-ktor-legacy` instead:

```kotlin
implementation("org.hildan.krossbow:krossbow-websocket-ktor-legacy:{{ git.tag }}")
implementation("io.ktor:ktor-client-cio:{{ versions.ktorLegacy }}")
```
