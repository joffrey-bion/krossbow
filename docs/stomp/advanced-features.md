## Receipts & Suspension

The STOMP protocol supports RECEIPT frames, allowing the client to know when the server has received a frame.
This only happens if a receipt header is set on the client frame.

If [auto-receipt](config.md#autoReceipt) is enabled, a `receipt` header is automatically generated and added to
all client frames supporting the mechanism, and for which a `receipt` header is not already present.
If auto-receipt is not enabled, a `receipt` header may still be provided manually in the parameters of some overloads.

When a `receipt` header is present (automatically added or manually provided), the method that is used to send the
frame suspends until the corresponding RECEIPT frame is received from the server.
If no RECEIPT frame is received from the server in the configured [time limit](config.md#receiptTimeout),
a `LostReceiptException` is thrown.

If no receipt is provided and auto-receipt is disabled, the method used to send the frame doesn't wait for a
RECEIPT frame and never throws `LostReceiptException`.
Instead, it returns immediately after the underlying web socket implementation is done sending the frame.
 
## Heart beats

When configured, heart beats can be used as a keep-alive to detect if the connection is lost.
The [heartBeat](config.md#heartBeat) property should be used to configure heart beats in the `StompClient`.

Note that the heart beats for the STOMP session are negotiated with the server.
The actual heart beats are defined by the CONNECTED frame received from the server as a result of the negotiation, and
may differ from the `StompClient` configuration.
The negotiation behaviour is [defined by the specification](https://stomp.github.io/stomp-specification-1.2.html#Heart-beating).

Sending and checking heart beats is automatically handled by `StompSession` implementations, depending on the result of 
the negotiation with the server.
If expected heart beats are not received in time, a `MissingHeartBeatException` is thrown and fails active subscriptions.

## Graceful disconnect

The graceful disconnect (or graceful shutdown) is a disconnection procedure
[defined by the STOMP specification](https://stomp.github.io/stomp-specification-1.2.html#DISCONNECT) to make sure the 
server gets all the frames before dropping the connection.

If [enabled in the config](config.md#gracefulDisconnect), when disconnecting from the server, the client first sends a 
DISCONNECT frame with a `receipt` header, and then waits for a RECEIPT frame before closing the connection.

If this graceful disconnect is disabled, then calling `StompSession.disconnect()` immediately closes the web
socket connection.
In this case, there is no guarantee that the server received all previous messages.

## Using custom headers in the web socket handshake

!!! warning "Not supported in browsers"
    The browser's `WebSocket` API does not support custom headers in the handshake (see
    [this open issue](https://github.com/whatwg/websockets/issues/16) in the web socket standard repo).
    Because of this, Krossbow cannot support this feature for the JS browser platform.
    However, the JS web socket client adapter is designed in a way that allows other implementations to support it,
    such as a Node.js implementation.

Some servers or connection flows may require extra HTTP headers in the web socket handshake.
The `StompClient.connect()` function doesn't support such headers out of the box, but this function is essentially
just a shorthand for connecting at the web socket level, and then connecting at the STOMP level.

In fact, we technically don't need to create and use a `StompClient` at all in order to use STOMP with Krossbow.
Krossbow provides a [WebSocketConnection.stomp()](../kdoc/krossbow-stomp-core/org.hildan.krossbow.stomp/stomp.html)
extension function that establishes a STOMP connection from an existing web socket connection.

We can leverage this to customize the web socket connection at will before connecting at STOMP level. For example:

```kotlin
val webSocketClient = WebSocketClient.builtIn() // or another web socket client

// connect at web socket level with custom headers
val wsSession = webSocketClient.connect(url, headers = mapOf("Custom-Header" to "custom-value"))

val config = StompConfig().apply {
    // here you can set up whatever config you would have done in the StompClient { ... } block
}
// connect at STOMP level on this open web socket, using the above config
val stompSession = wsSession.stomp(config)
```
