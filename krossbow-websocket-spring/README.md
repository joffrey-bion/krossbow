# Krossbow Web Socket Spring

This is a JVM implementation of the general Web Socket interface defined by `krossbow-websocket-core`, adapting the
`SpringWebSocketClient`.

This modules exposes a `SpringWebSocketClientAdapter` that can adapt any `SpringWebSocketClient` to Krossbow's
interface, for a maximum flexibility.

On top of that, 2 pre-configured clients are provided:

- `SpringDefaultWebSocketClient`: based on the `StandardWebSocketClient`, which makes use of the Tyrus implementation of
 the JSR-356 (`javax.websocket.*`).
- `SpringSockJSWebSocketClient`: an implementation using the SockJS protocol (requires a SockJS server), based on
 the standard web socket client, with the ability to fall back to other transports (like `RestTemplateXhrTransport`).

## Dependency

You will need to declare the `krossbow-websocket-spring` module dependency:

```
implementation("org.hildan.krossbow:krossbow-websocket-spring:$krossbowVersion")
```

## Usage with StompClient

### Predefined clients

To use one of the predefined Spring clients,
you need to specify it when creating your `StompClient`:

```kotlin
val stompClient = StompClient(SpringDefaultWebSocketClient)
```

Or using the default SockJS client:

```kotlin
val stompClient = StompClient(SpringSockJSWebSocketClient)
```

### Custom SpringWebSocketClient

You can also use your own `SpringWebSocketClient` by wrapping it in a `SpringWebSocketClientAdapter`:

```kotlin
// Pure Spring configuration
val springWsClient = StandardWebSocketClient().apply {
 taskExecutor = SimpleAsyncTaskExecutor("my-websocket-threads")
 userProperties = mapOf("my-prop" to "someValue")
}

// Krossbow adapter
val stompClient = StompClient(SpringWebSocketClientAdapter(springWsClient))
```

Another example of custom client, using Spring's SockJS client:

```kotlin
// Pure Spring configuration
val transports = listOf(
    WebSocketTransport(StandardWebSocketClient()),
    RestTemplateXhrTransport(myCustomRestTemplate),
)
val sockJsClient = SockJsClient(transports)

// Krossbow adapter
val stompClient = StompClient(SpringWebSocketClientAdapter(sockJsClient))
```