# Krossbow with Spring

Krossbow allows you to use
[Spring's `WebSocketClient`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/socket/client/WebSocketClient.html)
as transport for STOMP.

The `krossbow-websocket-spring` module provides the `.asKrossbowWebSocketClient()` extension, which adapts any of
Spring's `WebSocketClient` to Krossbow's web socket client interface.

For example, use `StandardWebSocketClient().asKrossbowWebSocketClient()` to wrap Spring's standard JSR-356 client.

## Usage with StompClient

To use a Spring web socket client with Krossbow's `StompClient`, adapt it to a Krossbow `WebSocketClient` using
`.asKrossbowWebSocketClient()` and pass it to the `StompClient` constructor:

```kotlin
val stompClient = StompClient(StandardWebSocketClient().asKrossbowWebSocketClient())
```

You can of course further customize your Spring client before adapting it to Krossbow:

```kotlin
// Pure Spring configuration
val springWsClient = StandardWebSocketClient().apply {
   taskExecutor = SimpleAsyncTaskExecutor("my-websocket-threads")
   userProperties = mapOf("my-prop" to "someValue")
}

// Krossbow adapter
val stompClient = StompClient(springWsClient.asKrossbowWebSocketClient())
```

Another example of custom client, using Spring's SockJS client:

```kotlin
// Pure Spring configuration
val transports = listOf(
    WebSocketTransport(StandardWebSocketClient()),
    RestTemplateXhrTransport(myCustomRestTemplate),
)
val springSockJsWsClient = SockJsClient(transports)

// Krossbow adapter
val stompClient = StompClient(springSockJsWsClient.asKrossbowWebSocketClient())
```

## Dependency information

You will need to declare the following Gradle dependency to use the Spring adapters:

```kotlin
implementation("org.hildan.krossbow:krossbow-websocket-spring:{{ git.short_tag }}")
```

It transitively depends on `spring-websocket`, so you don't need to add it yourself.

**Important:** if you're using Spring's `StandardWebSocketClient`, you'll also need to add a dependency on a
JSR-356 implementation, such as the Tyrus reference implementation:

```kotlin
implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client-jdk:{{ versions.tyrus }}")
```
