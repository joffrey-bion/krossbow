To quickly get started and use the built-in web socket clients, add the following Gradle dependency to your
`build.gradle(.kts)`:

```kotlin
implementation("org.hildan.krossbow:krossbow-stomp-default:{{ git.tag }}")
```

You can find more info about more advanced configuration [below](#gradle-setup).

## Basic usage (without body conversions)

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

If you want to disconnect automatically in case of exception or normal termination, you can use
a `try`/`finally` block, or use `StompSession.use()`, which is similar to `Closeable.use()`:

```kotlin
import kotlinx.coroutines.flow.*
import org.hildan.krossbow.stomp.*

val client = StompClient() // custom WebSocketClient and other config can be passed in here
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

## Gradle setup

### Using the built-in web socket clients

For the basic usage of STOMP with the built-in web socket clients, you only need the following Gradle dependency:

```kotlin
implementation("org.hildan.krossbow:krossbow-stomp-default:{{ git.tag }}")
```

This provides a [StompClient()](../kdoc/krossbow-stomp-default/org.hildan.krossbow.stomp/-stomp-client.html)
constructor that will automatically use the built-in web socket client for the current platform.

If you want to use STOMP body conversions to serialize/deserialize objects directly into/from STOMP frames, add the
relevant conversion module.
For instance, to use Kotlinx Serialization:

```kotlin
implementation("org.hildan.krossbow:krossbow-stomp-default:{{ git.tag }}")
implementation("org.hildan.krossbow:krossbow-stomp-kxserialization:{{ git.tag }}")
```

### Using third-party web socket clients

If you want to use another web socket client, declare both the core STOMP artifact and the specific web socket artifact.
For example, if you want to use Krossbow with Ktor web socket client:

```kotlin
implementation("org.hildan.krossbow:krossbow-stomp-core:{{ git.tag }}")
implementation("org.hildan.krossbow:krossbow-websocket-ktor:{{ git.tag }}")
```

This way you can call the [StompClient](../kdoc/krossbow-stomp-core/org.hildan.krossbow.stomp/-stomp-client/index.html)
constructor with the web socket client of your choice (e.g. `StompClient(KtorWebSocketClient())`).

If you want to use STOMP body conversions to serialize/deserialize objects directly into/from STOMP frames, replace the
`krossbow-stomp-core` module with the relevant conversion module.
For instance, to use Kotlinx Serialization:

```kotlin
implementation("org.hildan.krossbow:krossbow-stomp-kxserialization:{{ git.tag }}")
implementation("org.hildan.krossbow:krossbow-websocket-ktor:{{ git.tag }}")
```
