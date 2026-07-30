## Configuring the StompClient

The `StompClient` can be configured at construction time using a convenient lambda block:

```kotlin
val stompClient = StompClient(WebSocketClient.builtIn()) {
    connectionTimeout = 3.seconds
    gracefulDisconnect = false
}
```

You can also create the configuration separately and then pass it when constructing the client:

```kotlin
val stompConfig = StompConfig().apply {
    connectionTimeout = 3.seconds
    gracefulDisconnect = false
}

val stompClient = StompClient(WebSocketClient.builtIn(), stompConfig)
```

## Configuration options

You can find out about all configuration properties in the
[StompConfig KDoc](../kdoc/krossbow-stomp-core/org.hildan.krossbow.stomp.config/-stomp-config/index.html).
