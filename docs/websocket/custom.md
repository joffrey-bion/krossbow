# Implement Krossbow's WebSocketClient yourself

The `krossbow-websocket-core` module defines a standard web socket API abstraction that is used by the STOMP artifacts
and that you can also use directly if you're only interested in the web socket protocol without STOMP.

Krossbow provides built-in implementations of the web socket interfaces in
[the `krossbow-websocket-builtin` module](builtin.md), but you can of course implement your own.

## Basics

You can create your own implementation of Krossbow's web socket client by implementing the
[WebSocketClient](../kdoc/krossbow-websocket-core/org.hildan.krossbow.websocket/-web-socket-client/index.html) interface.

This interface simply has a
[connect()](../kdoc/krossbow-websocket-core/org.hildan.krossbow.websocket/-web-socket-client/connect.html) method
returning an instance of
[WebSocketConnection](../kdoc/krossbow-websocket-core/org.hildan.krossbow.websocket/-web-socket-connection/index.html).
The `WebSocketConnection` actually contains the bulk of the web socket interactions implementation.

Please follow the KDoc of these interfaces to learn more about the contract that needs to be satisfied for each method.

## Helpers

The `krossbow-websocket-core` module doesn't only provide interfaces to implement.
It also provides some helper classes that help with most implementations of those interfaces.

The [WebSocketListenerFlowAdapter](../kdoc/krossbow-websocket-core/org.hildan.krossbow.websocket/-web-socket-listener-flow-adapter/index.html)
allows to adapt listener-based web socket APIs to Krossbow's `Flow` API easily.
It takes care of partial message handling automatically, and can provide backpressure on the callback caller thanks to
its `suspend` callbacks.

The [UnboundedWsListenerFlowAdapter](../kdoc/krossbow-websocket-core/org.hildan.krossbow.websocket/-unbounded-ws-listener-flow-adapter/index.html)
also adapts listener-based APIs to Krossbow's flow, but without any backpressure support (functions are not `suspend`
and return immediately). It adds new messages to an unbounded queue.
This is necessary with some APIs like JS browsers `WebSocket` API, which
[cannot apply backpressure](https://web.dev/websocketstream/#applying-backpressure-to-received-messages-is-impossible)
in any way on their web socket traffic.

## Dependency information

Add the following to your `build.gradle(.kts)` in order to get the Krossbow's interfaces and helpers:

```kotlin
implementation("org.hildan.krossbow:krossbow-websocket-core:{{ git.tag }}")
```
