# Krossbow

[![Bintray Download](https://img.shields.io/bintray/v/joffrey-bion/maven/krossbow-client.svg?label=bintray)](https://bintray.com/joffrey-bion/maven/krossbow-client/_latestVersion)
[![Travis Build](https://img.shields.io/travis/joffrey-bion/krossbow/master.svg)](https://travis-ci.org/joffrey-bion/krossbow)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/joffrey-bion/krossbow/blob/master/LICENSE)

A coroutine-based Kotlin multiplatform WebSocket client and [STOMP 1.2](https://stomp.github.io/index.html) client
 over web sockets.

***This project is experimental, it's still in its early development stage.***

## Usage

### Raw STOMP usage (without conversions)

This is how to create a client and interact with it (the verbose way):

```kotlin
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession

val client = StompClient() // custom WebSocketClient and other config can be passed in here
val session: StompSession = client.connect(url) // optional login/passcode can be provided here

try {
    session.sendText("/some/destination", "Basic text message") 

    val subscription = session.subscribeText("/some/topic/destination")
    val firstMessage: String? = subscription.messages.receive()

    println("Received: $firstMessage")
    subscription.unsubscribe()
} finally {
    jsonStompSession.disconnect()
}
```

If the STOMP session is only used in one place like this, we can get rid of the `try`/`catch`, and `disconnect()` 
automatically by calling `StompSession.use()` (similar to `Closeable.use()`):

```kotlin
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.StompClient

StompClient().connect(url).use { // this: StompSessionWithKxSerialization
    session.sendText("/some/destination", "Basic text message") 

    val subscription = session.subscribeText("/some/topic/destination")
    val firstMessage: String? = subscription.messages.receive()

    println("Received: $firstMessage")
    subscription.unsubscribe()
}
```

### Using body conversions

Usually STOMP is used in conjonction with JSON bodies that are converted back and forth between objects.
Krossbow comes with built-in support for Kotlinx Serialization in order to support multiplatform conversions.

Call `withJsonConversions` to add conversions capabilities to your `StompSession`.
Then, use `convertAndSend` and `subscribe` overloads with serializers to use these conversions:

```kotlin
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.conversions.kxserialization.convertAndSend
import org.hildan.krossbow.stomp.conversions.kxserialization.subscribe
import org.hildan.krossbow.stomp.conversions.kxserialization.withJsonConversions

val session = StompClient().connect(url)
val jsonStompSession = session.withJsonConversions() // adds convenience methods for kotlinx.serialization's conversions

jsonStompSession.use {
    convertAndSend("/some/destination", MyPojo("Custom", 42), MyPojo.serializer()) 

    val subscription = subscribe("/some/topic/destination", MyMessage.serializer())
    val firstMessage: MyMessage = subscription.messages.receive()

    println("Received: $firstMessage")
    subscription.unsubscribe()
}
```

Note that `withJsonConversions()` takes an optional `Json` argument to customize the serialization configuration.

#### Using Jackson on the JVM

If you're only targeting the JVM, you can use Jackson instead of Kotlinx Serialization to use reflection instead of
 manually provided serializers:
 
```kotlin
StompClient().connect(url).withJacksonConversions().use {
    convertAndSend("/some/destination", MyPojo("Custom", 42)) 

    val subscription = subscribe<MyMessage>("/some/topic/destination")
    val firstMessage: MyMessage = subscription.messages.receive()

    println("Received: $firstMessage")
    subscription.unsubscribe()
}
```

You can use it with your own `ObjectMapper` this way:

```kotlin
val objectMapper = jacksonObjectMapper().enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
val session = StompClient().connect(url).withJacksonConversions(objectMapper)
```

## Adding the dependency

All the dependencies are currently published to Bintray JCenter.
They are not yet available on npm yet.

### Common library

```kotlin
// common source set
implementation("org.hildan.krossbow:krossbow-stomp-metadata:$krossbowVersion")

// jvm source set
implementation("org.hildan.krossbow:krossbow-stomp-jvm:$krossbowVersion")

// js source set
implementation("org.hildan.krossbow:krossbow-stomp-js:$krossbowVersion")
```

## Project structure
 
This project contains the following modules:
- `krossbow-stomp`: the multiplatform STOMP client to use as a STOMP library in common, JVM or JS projects. It
 implements the STOMP 1.2 protocol on top of a websocket API defined by the `krossbow-websocket-api` module.
- `krossbow-websocket-api`: a common WebSocket API that the STOMP client relies on, to enable the use of custom
 WebSocket clients. This also provides a default JS client implementations using the Browser's native WebSocket.
- `krossbow-websocket-sockjs`: a multiplatform `WebSocketClient` implementation for use with SockJS servers. It uses
 Spring's SockJSClient on JVM, and npm `sockjs-client` for JavaScript (NodeJS and browser).
- `krossbow-websocket-spring`: a JVM implementation of the web socket API using Spring's WebSocketClient. Provides
 both a normal WebSocket client and a SockJS one.
