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
