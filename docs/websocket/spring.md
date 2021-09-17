# Krossbow with Spring

Krossbow allows you to use [Spring's `WebSocketClient`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/socket/client/WebSocketClient.html) as transport for STOMP.

The `krossbow-websocket-spring` module provides the `SpringWebSocketClientAdapter`, which adapts any of Spring's 
`WebSocketClient` to Krossbow's web socket interface.

On top of that, some pre-configured clients are provided:

- `SpringDefaultWebSocketClient`: based on the `StandardWebSocketClient`, which relies on the
  [JSR-356](https://www.oracle.com/technical-resources/articles/java/jsr356.html) (`javax.websocket.*`) web socket standard.
- `SpringSockJSWebSocketClient`: an implementation using the SockJS protocol (requires a SockJS server), based on
  the standard web socket client, with the ability to fall back to other transports (like `RestTemplateXhrTransport`).
- `SpringJettyWebSocketClient`: based on the `JettyWebSocketClient` (requires Jetty dependency, see below)

## Usage with StompClient

### Predefined clients

To use one of the predefined Spring clients, you need to specify it when creating your `StompClient`:

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

## Dependency information

You will need to declare the following Gradle dependency to use the Spring adapters:

```kotlin
implementation("org.hildan.krossbow:krossbow-websocket-spring:{{ git.tag }}")
```

If you're using the `SpringJettyWebSocketClient`, you'll need to add a dependency on Jetty's web socket client:

```kotlin
implementation("org.eclipse.jetty.websocket:websocket-client:9.4.43.v20210629")
```
