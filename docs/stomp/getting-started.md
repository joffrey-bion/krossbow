## Choosing a web socket implementation

Krossbow uses web sockets as transport for the STOMP communication.
Multiple web socket client implementations are supported.

Check out the [web socket client table](../index.md#web-socket-clients-target-support) to help you choose a web socket
implementation based on the platforms you need to support.
You can find more information about each client in their respective section of this doc.

We recommend the built-in client adapters if they cover the Kotlin targets you need to support, in order to
limit 3rd party dependencies. Otherwise, Ktor is a good choice if you don't have special needs like SockJS.

## Dependencies setup

For the basic usage of STOMP without serialization, add the `krossbow-stomp-core` dependency as well as the web socket
module of your choice.

For example to use STOMP with the built-in web socket client:

```kotlin
implementation("org.hildan.krossbow:krossbow-stomp-core:{{ git.tag }}")
implementation("org.hildan.krossbow:krossbow-websocket-builtin:{{ git.tag }}")
```

For other web socket clients, check out their dedicated documentation page to find out which Krossbow dependencies are
needed.

!!! tip "The rest of this guide uses the built-in client."

## Basic usage (without body conversions)

This is how to create a STOMP client and interact with it:

```kotlin
val client = StompClient(WebSocketClient.builtIn()) // other config can be passed in here
val session: StompSession = client.connect(url) // optional login/passcode can be provided here

// Send text messages using this convenience function
session.sendText(destination = "/some/destination", body = "Basic text message")

// Sometimes no message body is necessary
session.sendEmptyMsg(destination = "/some/destination") 

// This subscribe() call triggers a SUBSCRIBE frame
// and returns the flow of messages for the subscription
val subscription: Flow<String> = session.subscribeText("/some/topic/destination")

// Use an appropriate coroutine 'scope' to collect the received frames
val collectorJob = scope.launch {
    subscription.collect { msg ->
        println("Received: $msg")
    }
}
delay(3000)
// cancelling the flow collector triggers an UNSUBSCRIBE frame
collectorJob.cancel()
 
session.disconnect()
```

If you want to disconnect automatically in case of exception or normal termination, you can use
a `try`/`finally` block, or use `StompSession.use()`, which is similar to `Closeable.use()`:

```kotlin
import kotlinx.coroutines.flow.*
import org.hildan.krossbow.stomp.*
import org.hildan.krossbow.websocket.*
import org.hildan.krossbow.websocket.builtin.*

val client = StompClient(WebSocketClient.builtIn()) // other config can be passed in here
val session: StompSession = client.connect(url) // optional login/passcode can be provided here

session.use { s ->
    s.sendText("/some/destination", "Basic text message") 

    val subscription: Flow<String> = s.subscribeText("/some/topic/destination")

    // terminal operators that finish early (like first) also trigger UNSUBSCRIBE automatically
    val firstMessage: String = subscription.first()
    println("Received: $firstMessage")
}
// DISCONNECT frame was automatically sent at the end of the use{...} block
```

## Using body conversions

You can use STOMP with basic text as frame bodies, but it really becomes interesting when you can
convert the frame bodies back and forth into Kotlin objects.

Check out the following sections to see how to automatically convert your objects into STOMP frame bodies:

 * [using Kotlinx Serialization](./conversions/kx-serialization.md) (multiplatform)
 * [using Jackson](./conversions/jackson.md) (JVM-only)
 * [using Moshi](./conversions/moshi.md) (JVM-only)
 * [using custom conversions](./conversions/custom.md)
