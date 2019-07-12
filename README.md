# Krossbow

[![Bintray Download](https://img.shields.io/bintray/v/joffrey-bion/maven/krossbow-client.svg?label=bintray)](https://bintray.com/joffrey-bion/maven/krossbow-client/_latestVersion)
[![Travis Build](https://img.shields.io/travis/joffrey-bion/krossbow/master.svg)](https://travis-ci.org/joffrey-bion/krossbow)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/joffrey-bion/krossbow/blob/master/LICENSE)

A coroutine-based Kotlin multiplatform [STOMP 1.2](https://stomp.github.io/index.html) client over websockets.

The current implementation simply defines a common API for the STOMP client, as well as adapter interfaces to plug in
 a platform-specific STOMP implementation.

In the long run, it would be nice to 
[write the complete STOMP implementation in common code](https://github.com/joffrey-bion/krossbow/issues/5), and simply abstract the
 websocket API, so that it can be plugged into anything, including Ktor's websocket API.
 If you'd like to see it happen, don't hesitate to upvote the issue.
 
This project contains the following modules:
- `krossbow-client`: the multiplatform client to use as a STOMP library in common, JVM or JS projects. It basically
 acts as a facade on top of other modules, for easier dependency declaration.
- `krossbow-engine-api`: the API that engines must conform to in order to be used by the multiplatform client
- `krossbow-engine-spring`: a JVM implementation of the engine API using Spring's WebsocketStompClient as backend
- `krossbow-engine-webstompjs`: a JavaScript implementation of the engine API using 
[webstomp-client](https://github.com/JSteunou/webstomp-client) as backend

## Using Krossbow

This is how to create a client and interact with it (the verbose way):

```kotlin
val client = KrossbowClient()
val session = client.connect(url) // optional login/passcode can be provided here

try {
    session.send("/some/destination", MyPojo("Custom", 42)) 

    val subscription = session.subscribe<Message>("/some/topic/destination")
    val firstMessage = subscription.messages.receive()

    println("Received: $firstMessage")
    subscription.unsubscribe()
} finally {
    session.disconnect()
}
```

We can make things simpler, though:

- To get the `disconnect()` automatically, you can use `useSession` (similar to `Closeable.use()`)
- If `unsubscribe()` is not essential, you can actually destructure the `Subscription` object

Here's how it looks now:

```kotlin
KrossbowClient().useSession(url) { // this: KrossbowSession

    send("/some/destination", CustomObject("Typed values", 42))

    val (messages) = subscribe<Message>("/some/topic/destination")

    val firstMessage = messages.receive()
    println("Received: $firstMessage")
}
```

## Adding the dependency

All the dependencies are currently published to Bintray JCenter.
Unfortunately, they are not yet available on npm.

### Common library

```kotlin
// common source set
implementation("org.hildan.krossbow:krossbow-client-metadata:$krossbowVersion")

// jvm source set
implementation("org.hildan.krossbow:krossbow-client-jvm:$krossbowVersion")

// js source set
implementation("org.hildan.krossbow:krossbow-client-js:$krossbowVersion")
```
