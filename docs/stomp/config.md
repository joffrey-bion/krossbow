## Configuring the StompClient

The `StompClient` can be configured at construction time using a convenient lambda block:

```kotlin
val stompClient = StompClient {
    connectionTimeoutMillis = 3000
    gracefulDisconnect = false
}
```

You can also create the configuration separately and then pass it when constructing the client:

```kotlin
val stompConfig = StompConfig().apply {
    connectionTimeoutMillis = 3000
    gracefulDisconnect = false
}

val stompClient = StompClient(config = stompConfig)
```

## Configuration options

`autoReceipt` (default: false) { #autoReceipt }
:   Whether to automatically attach a `receipt` header to the sent frames in order to track receipts.

`autoContentLength` (default: true)
:   Whether to automatically compute and add the `content-length` header in sent frames.

`connectWithStompCommand` (default: false)
:   Whether to use the `STOMP` command instead of `CONNECT` to establish the connection.

    Clients that use the `STOMP` frame instead of the `CONNECT` frame will only be able to connect to STOMP 1.2
    servers (as well as some STOMP 1.1 servers) but the advantage is that a protocol sniffer/discriminator will be
    able to differentiate the STOMP connection from an HTTP connection.

`heartBeat` (default: none) { #heartBeat }
:   The heart beats to request for the STOMP sessions.

    This is part of a negotiation and does not imply that this exact heart beat configuration will be used.
    The actual heart beats are defined by the CONNECTED frame received from the server as a result of the
    negotiation. This behaviour is
    [defined by the specification](https://stomp.github.io/stomp-specification-1.2.html#Heart-beating).

`heartBeatTolerance` (default: none)
:   Defines tolerance for heart beats.

    If both the client and server really stick to the heart beats periods negotiated and given by the CONNECTED frame,
    network latencies will make them miss their marks. That's why we need some sort of tolerance.
    
    In case the server is too strict about its expectations, we can send heart beats a little bit earlier than we're
    supposed to (see `HeartBeatTolerance.outgoingMarginMillis`).
    
    In case the server really sticks to its own period without such margin, we need to allow a little bit of delay to
    make up for network latencies before we fail and close the connection (see `HeartBeatTolerance.incomingMarginMillis`).

`connectionTimeoutMillis` (default: 15000)
:   Defines how long to wait for the websocket+STOMP connection to be established before throwing an exception.

`receiptTimeoutMillis` (default: 1000) { #receiptTimeoutMillis }
:   Defines how long to wait for a RECEIPT frame from the server before throwing a `LostReceiptException`.
    Only crashes when a `receipt` header was actually present in the sent frame (and thus a RECEIPT was expected).
    Such header is always present if `autoReceipt` is enabled.

    Note that this doesn't apply to the DISCONNECT frames, use `disconnectTimeoutMillis` instead for that.

`disconnectTimeoutMillis` (default: 200)
:   Like `receiptTimeoutMillis` but only for the receipt of the DISCONNECT frame.
    This is ignored if `gracefulDisconnect` is disabled.

    Note that if this timeout expires, the [StompSession.disconnect] call doesn't throw an exception.
    This is to allow servers to close the connection quickly (sometimes too quick for sending a RECEIPT/ERROR) as
    [mentioned in the specification](http://stomp.github.io/stomp-specification-1.2.html#DISCONNECT).

`gracefulDisconnect` (default: true) { #gracefulDisconnect }
:   Enables [graceful disconnect](https://stomp.github.io/stomp-specification-1.2.html#DISCONNECT).

    If enabled, when disconnecting from the server, the client first sends a DISCONNECT frame with a `receipt`
    header, and then waits for a RECEIPT frame before closing the connection.
    
    If this graceful disconnect is disabled, then calling [StompSession.disconnect] immediately closes the web
    socket connection.
    In this case, there is no guarantee that the server received all previous messages.

`instrumentation`
:   A set of hooks that are called in different places of the internal execution of Krossbow.
    The instrumentation can be used for monitoring, logging or debugging purposes.
