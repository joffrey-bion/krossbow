# Krossbow with OkHttp

Krossbow allows you to use [OkHttp's `WebSocket`](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-web-socket/) as transport for STOMP.

OkHttp is very popular on Android, and is already part of many projects as HTTP client of choice.

The `krossbow-websocket-okhttp` module provides the `OkHttpWebSocketClient`, which adapts OkHttp's `WebSocket` to
Krossbow's web socket interface.

## Usage with StompClient

To use the `OkHttpWebSocketClient` instead of the platform default,
you need to specify it when creating your `StompClient`:

```kotlin
val client = StompClient(OkHttpWebSocketClient())
```

You can customize the actual `OkHttpClient` used behind the scenes by passing it to `OkHttpWebSocketClient()`:

```kotlin
// This allows to configure the underlying OkHttpClient as you please
// (or use an existing one from your project)
val okHttpClient = OkHttpClient.Builder()
    .callTimeout(Duration.ofMinutes(1))
    .pingInterval(Duration.ofSeconds(10))
    .build()
val wsClient = OkHttpWebSocketClient(okHttpClient)
val stompClient = StompClient(wsClient)
```

## Dependency information

You will need to declare the following Gradle dependency to use the `OkHttpWebSocketClient`:

```kotlin
implementation("org.hildan.krossbow:krossbow-websocket-okhttp:{{ git.tag }}")
```
