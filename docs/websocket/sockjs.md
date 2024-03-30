# Krossbow with SockJS

Krossbow allows you to use SockJS-compatible clients as transport for STOMP.

The `krossbow-websocket-sockjs` is a multiplatform facade implementing Krossbow's web socket interface by relying on
different SockJS implementations. Here are the backing implementations for the different platforms:

- JS (browser and NodeJS): the [`sockjs-client`](https://github.com/sockjs/sockjs-client) library (isomorphic)
- JVM: Spring's `WebSocketClient` (with SockJS enabled), through [`krossbow-websocket-spring`](./spring.md)

!!! warning "Using a SockJS client requires a SockJS-enabled server."

## Usage with StompClient

To use this client, just call `SockJSClient()` and the relevant platform-specific client will be instantiated for you:

```kotlin
val client = StompClient(SockJSClient())
```

## Dependency information

You will need to declare the following Gradle dependency to use the `SockJSClient`:

```kotlin
implementation("org.hildan.krossbow:krossbow-websocket-sockjs:{{ git.short_tag }}")
```
