Here are some details about how to migrate from one major version to another.

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
