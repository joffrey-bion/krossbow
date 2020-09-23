# Krossbow STOMP Kotlinx Serialization

This is an extension of `krossbow-stomp-core` that provides convenient multiplatform type conversions using
[Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization).

The main additions that this module brings are the extension functions:
 
- `StompSession.withBinaryConversions()`
- `StompSession.withTextConversions()`
- `StompSession.withJsonConversions()` (requires a peer dependency on `kotlinx-serialization-json`, see below)

which turn your `StompSession` into a `StompSessionWithKxSerialization`.

This new session type has additional methods that use Kotlinx Serialization's serializers to serialize/deserialize your
objects using the format of your choice (JSON, protobuf, etc.):

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

## Dependency

You will need to declare the `krossbow-stomp-kxserialization` module dependency to add these capabilities (you don't 
need the core module anymore as it is transitively brought by this one):

```
// for a common module in a multiplatform project (no need to declare anything in JVM or JS source sets)
implementation("org.hildan.krossbow:krossbow-stomp-kxserialization:$krossbowVersion")

// for a JVM dependency in a JVM or Android project
implementation("org.hildan.krossbow:krossbow-stomp-kxserialization-jvm:$krossbowVersion")

// for a JS dependency in a JS project
implementation("org.hildan.krossbow:krossbow-stomp-kxserialization-js:$krossbowVersion")
```

To avoid adding unnecessary runtime dependencies on consumers of Krossbow, `kotlinx-serialization-json` is not
 transitively brought by `krossbow-stomp-serialization` (since Krossbow 0.42.0, using Kotlinx Serialization 1.0.0-RC2).
 
You need to add yourself the relevant Kotlinx Serialization dependency corresponding to the format you want to use.
In the case of JSON, that would be `kotlinx-serialization-json`.
