# Krossbow

[![Bintray Download](https://img.shields.io/bintray/v/joffrey-bion/maven/krossbow-stomp-core)](https://bintray.com/joffrey-bion/maven/krossbow-stomp-core/_latestVersion)
[![Github Build](https://img.shields.io/github/workflow/status/joffrey-bion/krossbow/CI%20Build?label=build&logo=github)](https://github.com/joffrey-bion/krossbow/actions?query=workflow%3A%22CI+Build%22)
[![Travis Build](https://img.shields.io/travis/joffrey-bion/krossbow/master.svg?label=build&logo=travis)](https://travis-ci.org/joffrey-bion/krossbow)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/joffrey-bion/krossbow/blob/master/LICENSE)

A coroutine-based Kotlin multi-platform WebSocket client and [STOMP 1.2](https://stomp.github.io/index.html) client
 over web sockets.

## Kotlin version compatibility

Starting at version 0.30.0, Krossbow uses Kotlin 1.4.
This library was built using Kotlin 1.3 up to version 0.21.1.

## Experimental status

***This project is experimental, meaning that there is no guarantee of backwards compatibility.*** 
Any part of the public API may change until version 1.0.0 is released.

This is mainly due to the fact that the project is young, but also because it has multiple dependencies on
experimental libraries like [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) and 
[Kotlinx IO](https://github.com/Kotlin/kotlinx-io).

## Supported targets

This project supports the JVM, browser and Node JS targets, and is tested against all 3.
Take a look at the different web socket implementations below to see how each target is supported and by which artifact.

Android 5.0+ (API level 21+) should be supported by using the OkHttp web socket artifact (`krossbow-websocket-okhttp`) and 
one of the JVM STOMP artifacts.
However, the Android tooling's desugaring is not tested as part of the build (which would add a lot of complexity), and thus 
any feedback on this use case is more than welcome.
Please upvote [the corresponding issue](https://github.com/joffrey-bion/krossbow/issues/49) if you'd like to see proper
tests and packaging for the Android target.

The Native target is currently unsupported, due to a lack of native web socket implementation.
Adding support for it may require a bit more effort.
Contributions are welcome in this respect, though.

## Supported STOMP features

All of the [STOMP 1.2](https://stomp.github.io/index.html) specification is implemented and supported:

- All STOMP frames, including `ACK`/`NACK` and transactions
- Custom headers where the protocol allows them
- Receipts (waiting for RECEIPT frame based on receipt header)
- Heart beats (keep alive)
- Text and binary bodies

Additional features:

- Auto-receipts (automatically adds RECEIPT headers when appropriate to ensure no frame is lost)
- Automatic content length header for sent frames
- Built-in JSON body conversions (Kotlinx Serialization or Jackson)
- Possibility to hook custom body converters (for textual or binary bodies)

If you find a bug or a feature that's missing compared to the specification, please open an issue.

## STOMP Usage

### Raw STOMP usage (without conversions)

This is how to create a STOMP client and interact with it:

```kotlin
import kotlinx.coroutines.flow.*
import org.hildan.krossbow.stomp.*

val client = StompClient() // custom WebSocketClient and other config can be passed in here
val session: StompSession = client.connect(url) // optional login/passcode can be provided here

session.sendText("/some/destination", "Basic text message") 

// this triggers a SUBSCRIBE frame and returns the flow of messages for the subscription
val subscription: Flow<String> = session.subscribeText("/some/topic/destination")

val collectorJob = launch {
    subscription.collect { msg ->
        println("Received: $msg")
    }
}
delay(3000)
// cancelling the flow collector triggers an UNSUBSCRIBE frame
collectorJob.cancel()
 
session.disconnect()
```

If you want to disconnect automatically in case of exception or normal termination, you can use a `try`/`finally` block,
or use `StompSession.use()`, which is similar to `Closeable.use()`:

```kotlin
import kotlinx.coroutines.flow.*
import org.hildan.krossbow.stomp.*

val client = StompClient() // custom WebSocketClient and other config can be passed in here
val session: StompSession = client.connect(url) // optional login/passcode can be provided here

session.use { // this: StompSession
    sendText("/some/destination", "Basic text message") 

    val subscription: Flow<String> = subscribeText("/some/topic/destination")

    // terminal operators that finish early (like first) also trigger UNSUBSCRIBE automatically
    val firstMessage: String = subscription.first()
    println("Received: $firstMessage")
}
// DISCONNECT frame was automatically sent at the end of the use{...} block
```

### Using body conversions

You can use STOMP with basic text as frame bodies, but it really becomes interesting when you can convert the frame
bodies back and forth into Kotlin objects.

#### Using Kotlinx Serialization conversions

Krossbow comes with built-in support for Kotlinx Serialization in order to support multiplatform conversions.

You will need to use the `krossbow-stomp-kxserialization` module to add these capabilities (you don't need the core
module anymore as it is transitively brought by this one).

Call `withJsonConversions` to add JSON conversions capabilities to your `StompSession`.
Then, use `convertAndSend` and `subscribe` overloads with serializers to use these conversions:

```kotlin
import org.hildan.krossbow.stomp.*
import org.hildan.krossbow.stomp.conversions.kxserialization.*

@Serializable
data class Person(val name: String, val age: Int)
@Serializable
data class MyMessage(val timestamp: Long, val author: String, val content: String)

val session = StompClient().connect(url)
val jsonStompSession = session.withJsonConversions() // adds convenience methods for kotlinx.serialization's conversions

jsonStompSession.use {
    convertAndSend("/some/destination", Person("Bob", 42), Person.serializer()) 

    // overloads without explicit serializers exist, but should be avoided if you also target JavaScript
    val subscription: Flow<MyMessage> = subscribe("/some/topic/destination", MyMessage.serializer())
    
    subscription.collect { msg ->
        println("Received message from ${msg.author}: ${msg.content}")
    }
}
```

Note that `withJsonConversions()` takes an optional `Json` argument to customize the serialization configuration.

You can also use the more general `withTextConversions()` and `withBinaryConversions()` methods with the various
serialization formats provided by Kotlinx Serialization.

#### Using Jackson conversions (JVM only)

If you're only targeting the JVM, you can use Jackson instead of Kotlinx Serialization to use reflection instead of
 manually provided serializers.
 
You will need to use the `krossbow-stomp-jackson` module to add these capabilities (you don't need the core
 module anymore as it is transitively brought by this one).

```kotlin
StompClient().connect(url).withJacksonConversions().use {
    convertAndSend("/some/destination", Person("Bob", 42)) 

    val subscription: Flow<MyMessage> = subscribe<MyMessage>("/some/topic/destination")
    val firstMessage: MyMessage = subscription.first()

    println("Received: $firstMessage")
}
```

#### Using custom text conversions

If you want to use your own text conversion, you can implement `TextMessageConverter` without any additional module
, and use `withTextConversions` to wrap your `StompSession` into a `StompSessionWithClassToTextConversions`.

**Warning:** reflection-based conversions are very likely to behave poorly on the JS platform. It is usually safer to
 rely on Kotlinx Serialization for multiplatform conversions.

```kotlin
val myConverter = object : TextMessageConverter {
    override val mimeType: String = "application/json;charset=utf-8"

    override fun <T : Any> convertToString(body: T, bodyType: KClass<T>): String {
        TODO("your own object -> text conversion")
    }

    override fun <T : Any> convertFromString(body: String, bodyType: KClass<T>): T {
        TODO("your own text -> object conversion")
    }
}

StompClient().connect(url).withTextConversions(myConverter).use {
    convertAndSend("/some/destination", MyPojo("Custom", 42)) 

    val subscription = subscribe<MyMessage>("/some/topic/destination")
    val firstMessage: MyMessage = subscription.first()

    println("Received: $firstMessage")
}
```

## Picking a web socket implementation

The `krossbow-websocket-core` artifact defines a general web socket API, and provides basic JS/JVM implementations
 without third-party dependencies.
Other artifacts provide more implementations supporting more platforms by depending on third party libraries:

| Artifact                    |           Browser          |           NodeJS           |                JVM8+ (blocking)               |   JVM11+ (async)   | Dependencies |
|-----------------------------|:--------------------------:|:--------------------------:|:---------------------------------------------:|:------------------:|--------------|
| `krossbow-websocket-core`   |     :white_check_mark:     |                            |                                               | :white_check_mark: |              |
| `krossbow-websocket-sockjs` | :eight_pointed_black_star: | :eight_pointed_black_star: |           :eight_pointed_black_star:          |                    | [sockjs-client](https://github.com/sockjs/sockjs-client), [Spring websocket](https://docs.spring.io/spring-framework/docs/5.0.0.BUILD-SNAPSHOT/spring-framework-reference/html/websocket.html) |
| `krossbow-websocket-spring` |                            |                            | :white_check_mark: :eight_pointed_black_star: |                    | [Spring websocket](https://docs.spring.io/spring-framework/docs/5.0.0.BUILD-SNAPSHOT/spring-framework-reference/html/websocket.html) |
| `krossbow-websocket-okhttp` |                            |                            |               :white_check_mark:              |                    | [OkHttp](https://square.github.io/okhttp/) |

:white_check_mark: supported with native web socket transport

:eight_pointed_black_star: supported using [SockJS](https://github.com/sockjs/sockjs-client) protocol (requires a SockJS server)

## Adding the dependency

All the modules are currently published to Bintray JCenter.
They are not available on Maven Central yet because of Javadoc generation issues/Dokka misconfiguration.
They are not yet available on npm yet.

If you are using STOMP and have no special requirements for the web socket implementation, `krossbow-websocket-core` 
doesn't need to be explicitly declared as dependency because it is transitively pulled by all `krossbow-stomp-xxx` 
artifacts.

Using Gradle for a single platform project, add the relevant artifact (with platform suffix):

```kotlin
// jvm project
implementation("org.hildan.krossbow:krossbow-stomp-core-jvm:$krossbowVersion")

// js project
implementation("org.hildan.krossbow:krossbow-stomp-core-js:$krossbowVersion")
```

Using Gradle for a multi-platform project, since Kotlin 1.4 (and Krossbow version 0.30.0), you don't need to use the
`metadata` artifact, nor do you need to declare all platform-dependent artifacts.
Just add the non-suffixed version of the artifact to your `commonMain` source set:

```kotlin
// multiplatform project (commonMain source set)
implementation("org.hildan.krossbow:krossbow-stomp-core:$krossbowVersion")
```

## Project structure
 
This project contains the following modules:
- `krossbow-stomp-core`: the multiplatform STOMP client to use as a STOMP library in common, JVM or JS projects. It
 implements the STOMP 1.2 protocol on top of a websocket API defined by the `krossbow-websocket-core` module.
- `krossbow-stomp-jackson`: a superset of `krossbow-stomp-core` adding JSON conversion features using Jackson (JVM only)
- `krossbow-stomp-kxserialization`: a superset of `krossbow-stomp-core` adding conversion features using Kotlinx
 Serialization library (multiplatform)
- `krossbow-websocket-core`: a common WebSocket API that the STOMP client relies on, to enable the use of custom
 WebSocket clients. This also provides a default JS client implementations using the Browser's native WebSocket, and
  a JVM 11+ implementation using the async WebSocket API.
- `krossbow-websocket-sockjs`: a multiplatform `WebSocketClient` implementation for use with SockJS servers. It uses
 Spring's SockJSClient on JVM, and npm `sockjs-client` for JavaScript (NodeJS and browser).
- `krossbow-websocket-spring`: a JVM 8+ implementation of the web socket API using Spring's WebSocketClient. Provides
 both a normal WebSocket client and a SockJS one.
- `krossbow-websocket-okhttp`: a JVM implementation of the web socket API using OkHttp's WebSocketClient.

## Contribute

Don't hesitate to open GitHub issues, even to ask questions or discuss a new feature.
Pull-requests are welcome, but please open an issue first so that we can discuss the initial design or fix, which
may avoid unnecessary work.

## License

This project is published under the [MIT license](https://github.com/joffrey-bion/krossbow/blob/master/LICENSE).
