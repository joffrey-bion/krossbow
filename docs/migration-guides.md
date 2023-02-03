Here are some details about how to migrate from one major version to another.

## From 4.x to 5.x

### End of Ktor 1.x support

`krossbow-websocket-ktor-legacy` artifact was removed.
This means Krossbow no longer works with Ktor 1.x.
If you were using this module, please [migrate to Ktor 2](https://ktor.io/docs/migrating-2.html), and use the 
non-legacy [krossbow-websocket-ktor](./websocket/ktor.md) module.

### Kotlin 1.8

This update of Krossbow brings Kotlin 1.8, which might bring some incompatible changes to the Kotlin stdlib.
Please check the [compatibility guide](https://kotlinlang.org/docs/compatibility-guide-18.html) if you were using an 
older version of Kotlin.

## From 3.x to 4.x

### withJsonConversions moved to its own module

If you were using Krossbow with `krossbow-stomp-kxserialization`, the `withJsonConversions` helper has moved to a new
module called `krossbow-stomp-kxserialization-json`.
This new module now transitively brings `kotlinx-serialization-json` so you don't need to depend on that one explicitly.

### Built-in web socket clients moved to their own module and default `StompClient` constructor removed

Up to (and including) version 3.x of Krossbow, the built-in web socket clients for the supported platforms were part
of the `krossbow-websocket-core` module.
This module provided a `WebSocketClient.Companion.default()` factory function to provide the built-in web socket
implementation of the current platform.
Likewise, the `krossbow-stomp-core` module provided a `StompClient` constructor that used the "default" 
built-in web socket implementation for the current platform.

This approach limited the targets supported by those 2 core modules, even though all of their functionality was 
target-agnostic.
In order to support all Kotlin platforms in pure Kotlin modules, the built-in websocket implementations had to be moved 
to a separate module, and the constructor without web socket client was moved to a separate module (and later removed
completely for simplicity).

Breaking dependency changes, in short:

* if you used `WebSocketClient.default()` from `krossbow-websocket-core`, or any of the built-in clients directly,
  simply change your dependency to `krossbow-websocket-builtin` instead.
* if you used the `StompClient()` constructor without WS client argument (using the default value), add an explicit 
  dependency on `krossbow-websocket-builtin` and pass the built-in client explicitly to the constructor:
  `StompClient(WebSocketClient.default())`.

Note: the `WebSocketClient.default()` function was since renamed `WebSocketClient.builtIn()` in newer versions.

If you used other web socket implementations than the built-in ones, you don't have to change anything to your 
dependencies.

## From 2.x to 3.x

### Use Durations instead of millis

`StompConfiguration` no longer uses amounts of milliseconds, but uses the `kotlin.time.Duration` API.
The `-Millis` suffixes for the relevant properties were therefore dropped and the types changed.

Before:

```kotlin
val stomp = StompClient {
    connectionTimeoutMillis = 2000
    receiptTimeoutMillis = 5000
    disconnectTimeoutMillis = 300
}
```

After:
```kotlin
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val stomp = StompClient {
    connectionTimeout = 2.seconds
    receiptTimeout = 5.seconds
    disconnectTimeout = 300.milliseconds
}
```

### Flow instead of Channel in WebSocketConnection

If you used the websocket API directly, the `incomingFrames` channel is now a `Flow`.

Before:

```kotlin
val conn = wsClient.connect(url)
for (frame in conn.incomingFrames) {
    // do stuff
}
```

After:

```kotlin
val conn = wsClient.connect(url)
conn.incomingFrames.collect {
    // do stuff
}
```

### Tyrus no longer embedded in `krossbow-websocket-spring`

`krossbow-websocket-spring` no longer transitively brings a dependency on Tyrus.

If you didn't add any JSR-356 implementation manually, you now have to explicitly depend on one.
If you want the same behaviour as before, add the Tyrus dependency to your `build.gradle.kts` as follows:

```
dependencies {
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client-jdk:{{ versions.tyrus }}")
}
```

## From 1.x to 2.x

### `StompSession.use` now passes the session as `it`, not `this`

In order to align with [Closeable.use](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/use.html), the lambda for
`StompSession.use` now receives the session as an argument (`it`) and not receiver (`this`).

Before:

```kotlin
StompClient().connect(url).use {
    sendText("/dest", "message")
}
```

After:

```kotlin
StompClient().connect(url).use {
    it.sendText("/dest", "message")
}
// or
StompClient().connect(url).use { session ->
    session.sendText("/dest", "message")
}
```
