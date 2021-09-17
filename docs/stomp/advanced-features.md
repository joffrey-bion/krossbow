## Receipts & Suspension

The STOMP protocol supports RECEIPT frames, allowing the client to know when the server has received a frame.
This only happens if a receipt header is set on the client frame.

If [auto-receipt](config.md#autoReceipt) is enabled, a `receipt` header is automatically generated and added to
all client frames supporting the mechanism, and for which a `receipt` header is not already present.
If auto-receipt is not enabled, a `receipt` header may still be provided manually in the parameters of some overloads.

When a `receipt` header is present (automatically added or manually provided), the method that is used to send the
frame suspends until the corresponding RECEIPT frame is received from the server.
If no RECEIPT frame is received from the server in the configured [time limit](config.md#receiptTimeoutMillis),
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
