# Krossbow Web Socket Core

The `krossbow-websocket-core` module defines a standard web socket API abstraction.

Different web socket implementations can be used in Krossbow as long as they match the interfaces defined here.

## Built-in implementations

This core module already adapts some built-in implementations on each platform to this common interface:

- JS: the browser's native [WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API)
- JVM: the built-in JDK11+ asynchronous
  [java.net.http.WebSocket](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/WebSocket.html)

Note that these built-in implementations don't bring in any transitive dependencies.
The ones that do need dependencies are part of different modules, so that they can be included separately only if
needed.

## Creating your own implementation

You can create your own implementation of Krossbow's web socket client by implementing the
`org.hildan.krossbow.websocket.WebSocketClient` interface.

This interface simply has a `connect()` method returning an instance of `WebSocketConnection`.
The `WebSocketConnection` actually contains the bulk of the web socket interactions implementation.

Please follow the KDoc of these interfaces to learn more about the contract that needs to be satisfied for each method.

## Dependency information

You can add the following to your `build.gradle`:

```kotlin
implementation("org.hildan.krossbow:krossbow-websocket-core:{{ git.tag }}")
```

!!! warning "You probably don't need it"
    The `krossbow-websocket-core` dependency is transitively brought by all STOMP modules, so you only need it if you're
    exclusively interested in the bare Web Socket features without STOMP protocol. 