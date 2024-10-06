Here are some details about how to migrate from one major version to another.

## From 7.x to 8.x

### Headers rework

The headers types have been vastly reworked, for multiple reasons described in issue
[#518](https://github.com/joffrey-bion/krossbow/issues/518).

The following source-incompatible changes have been made:

* The `StompHeaders` interface no longer extends `MutableMap`, and is no longer mutable.
  * for modifications, use the `copy` functions of different header types to create a copy of the instance with some changes.
  * for access, use the typed properties for standard headers, and the `get(headerName)` function to get custom headers.
  * for access as a map (e.g. iteration), please open an issue describing your use case.
    The experimental `asMap()` function can be used in the meantime. 
* Header types were converted from `data class` to `interface`.
* Header constructors with typed parameters were replaced with factory functions of the same name.
  These functions take the mandatory headers as arguments, and the optional headers can be set via a lambda parameter.
* Header constructors that used to take a `rawHeaders: StompHeaders` argument were removed.
  If you need this functionality, please open an issue describing your use case.

## From 6.x to 7.x

### Web socket subprotocol negotiation

As part of issue [#493](https://github.com/joffrey-bion/krossbow/issues/493), a separate `protocols` parameter was added
to `WebSocketClient.connect` to enable web socket subprotocol negotiation.

Binary compatibility is preserved through some hidden synthetic functions. 
However, source compatibility isn't: usages of the `connect()` method that passed custom headers without a named `headers` parameter
will no longer compile. Adding the `headers =` parameter name will solve the issue.

For full negotiation support, clients need to be aware of which subprotocol the server chose to speak. This is why the
`protocol` property was added to `WebSocketConnection` ([#498](https://github.com/joffrey-bion/krossbow/issues/498)).
Implementers of this interface must implement this property. There shouldn't be many 3rd party implementations of the
connection interface, so binary compatibility should not be a real issue here.

### STOMP web socket subprotocol negotiation

Some servers like ActiveMQ require negotiating the STOMP protocol as a web socket subprotocol during the web socket
handshake (see issue [#492](https://github.com/joffrey-bion/krossbow/issues/492)), and cannot work otherwise.

**Breaking change:** To make the experience smoother, Krossbow v7.0.0 now automatically sends STOMP subprotocols (in all
supported versions) during the web socket handshake via the `Sec-WebSocket-Protocol` header.

If your server doesn't support it, you can customize the web socket handshake by manually connecting using
`WebSocketClient.connect()` with the parameters that suit you best, and then connect at STOMP level using the
`WebSocketConnection.stomp()` extension (without the need for a `StompClient` at all):

```kotlin
val client: WebSocketClient = TODO("get some web socket client implementation")
val wsConnection = client.connect(url) // without any subprotocols
val stompConfig = StompConfig().apply { 
    // set your config here if needed
}
val stompSession = wsConnection.stomp(config)
```

### No `host` header sent for STOMP 1.0

Thanks to the aforementioned changes, we can now detect the STOMP protocol version used by the server before sending
the first STOMP frame (`CONNECT` or `STOMP`).
If STOMP 1.0 is detected as web socket subprotocol during the web socket handshake, we no longer send by default the
`host` header which was introduced in 1.1 (and effectively breaks some old servers, see
[#122](https://github.com/joffrey-bion/krossbow/issues/122)). It can still be sent by manually specifying it of course.

### STOMP protocol version negotiation

The STOMP protocol itself supports negotiation of the version via headers in the `CONNECT` (or `STOMP`) frame.
So far, Krossbow only specified `1.2` as supported version. From now on, all 3 versions `1.0`, `1.1`, and `1.2` are
advertised as supported by the client.

If necessary, this behavior can be overridden by sending the `accept-version` header manually in
`customStompConnectHeaders`.

Because the protocol version can be negotiated both via web socket subprotocol and at STOMP level, there could
potentially be a mismatch. If this happens, the `connect()` call throws an exception. This can be disabled with
`StompConfig.failOnStompVersionMismatch`.

### `StompClient.connect()` now throws a different `WebSocketConnectionException`

The `org.hildan.krossbow.stomp.WebSocketConnectionException` is deprecated in favor of
`org.hildan.krossbow.websocket.WebSocketConnectionException`.
That exception has been around for a few years now and encapsulates all connection failures from different client
implementations already, so there is no need for a similar exception at the `StompClient` level.

If you used to catch this exception, make sure to update your import (the error-level deprecation should mitigate
any risk of missing the new exception).

## From 5.x to 6.x

### Switch to `kotlinx-io` and the `ByteString` type

The `kotlinx-io` library has been revamped, with an implementation that closely matches Okio now.
Krossbow has now internally switched from Okio to `kotlinx-io` as a result, but this part should have no visible effect
for the consumers of the library.

However, since `kotlinx-io` is a somewhat "standard" extension library for Kotlin, Krossbow can now more legitimately
use its `ByteString` type in public APIs that handled binary data (for both web socket and STOMP sessions).
This type represents immutable sequences of bytes, which is more convenient API-wise than byte arrays.

You will have to switch to these types if you used binary-based APIs.

### Deprecations cleanup

This is a major version, and therefore we allowed ourselves some cleanup by removing a bunch of deprecated APIs.
If you see unresolved references and are not sure how to fix them, please switch back to the Krossbow version 5 and fix
deprecation warnings by following the corresponding instructions.

Please check the [changelog](https://github.com/joffrey-bion/krossbow/blob/main/CHANGELOG.md) for the list of removals.

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
If you want the same behavior as before, add the Tyrus dependency to your `build.gradle.kts` as follows:

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
