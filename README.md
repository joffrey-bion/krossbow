# Krossbow

[![Bintray Download](https://img.shields.io/bintray/v/joffrey-bion/maven/krossbow-stomp-core.svg?label=bintray)](https://bintray.com/joffrey-bion/maven/krossbow-stomp-core/_latestVersion)
[![Travis Build](https://img.shields.io/travis/joffrey-bion/krossbow/master.svg)](https://travis-ci.org/joffrey-bion/krossbow)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/joffrey-bion/krossbow/blob/master/LICENSE)

A coroutine-based Kotlin multi-platform WebSocket client and [STOMP 1.2](https://stomp.github.io/index.html) client
 over web sockets.

***This project is experimental, meaning that there is no guarantee of backwards compatibility.***

## STOMP Usage

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

You will need to use the `krossbow-stomp-kxserialization` module to add these capabilities (you don't need the core
 module anymore as it is transitively brought by this one).

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
 manually provided serializers.
 
You will need to use the `krossbow-stomp-jackson` module to add these capabilities (you don't need the core
 module anymore as it is transitively brought by this one).

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

## Picking a web socket implementation

The `krossbow-websocket-api` defines a general web socket API, and provides a basic JS implementation using the
 Browser's native web socket.
Other artifacts provide more implementations supporting more platforms by depending on third party libraries:

| Artifact                    |           Browser          |           NodeJS           |                JVM8+ (blocking)               | JVM11+ (async) | Dependencies                                                                                                                                                                                   |
|-----------------------------|:--------------------------:|:--------------------------:|:---------------------------------------------:|:--------------:|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `krossbow-websocket-api`    |     :white_check_mark:     |                            |                                               |                |                                                                                                                                                                                                |
| `krossbow-websocket-sockjs` | :eight_pointed_black_star: | :eight_pointed_black_star: |           :eight_pointed_black_star:          |                | [sockjs-client](https://github.com/sockjs/sockjs-client), [Spring websocket](https://docs.spring.io/spring-framework/docs/5.0.0.BUILD-SNAPSHOT/spring-framework-reference/html/websocket.html) |
| `krossbow-websocket-spring` |                            |                            | :white_check_mark: :eight_pointed_black_star: |                | [Spring websocket](https://docs.spring.io/spring-framework/docs/5.0.0.BUILD-SNAPSHOT/spring-framework-reference/html/websocket.html)                                                           |

:white_check_mark: supported with native web socket transport

:eight_pointed_black_star: supported using [SockJS](https://github.com/sockjs/sockjs-client) protocol (requires a SockJS server)

## Adding the dependency

All the dependencies are currently published to Bintray JCenter.
They are not yet available on npm yet.

### Common library

```kotlin
// common source set
implementation("org.hildan.krossbow:krossbow-stomp-core:$krossbowVersion")

// jvm source set
implementation("org.hildan.krossbow:krossbow-stomp-core-jvm:$krossbowVersion")

// js source set
implementation("org.hildan.krossbow:krossbow-stomp-core-js:$krossbowVersion")
```

## Project structure
 
This project contains the following modules:
- `krossbow-stomp-core`: the multiplatform STOMP client to use as a STOMP library in common, JVM or JS projects. It
 implements the STOMP 1.2 protocol on top of a websocket API defined by the `krossbow-websocket-api` module.
- `krossbow-stomp-jackson`: a superset of `krossbow-stomp-core` adding conversion features using Jackson
- `krossbow-stomp-kxserialization`: a superset of `krossbow-stomp-core` adding conversion features using Kotlinx
 Serialization library
- `krossbow-websocket-api`: a common WebSocket API that the STOMP client relies on, to enable the use of custom
 WebSocket clients. This also provides a default JS client implementations using the Browser's native WebSocket.
- `krossbow-websocket-sockjs`: a multiplatform `WebSocketClient` implementation for use with SockJS servers. It uses
 Spring's SockJSClient on JVM, and npm `sockjs-client` for JavaScript (NodeJS and browser).
- `krossbow-websocket-spring`: a JVM implementation of the web socket API using Spring's WebSocketClient. Provides
 both a normal WebSocket client and a SockJS one.
