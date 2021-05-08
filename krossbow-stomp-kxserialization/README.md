# Krossbow STOMP Kotlinx Serialization

This is an extension of `krossbow-stomp-core` that provides convenient multiplatform type conversions using
[Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization).

The main additions that this module brings are the extension functions:
 
- `StompSession.withBinaryConversions()`
- `StompSession.withTextConversions()`
- `StompSession.withJsonConversions()` (requires a peer dependency on `kotlinx-serialization-json`, see below)

which turn your `StompSession` into a `StompSessionWithKxSerialization`.

This new session type has additional methods that use Kotlinx Serialization's serializers to serialize/deserialize your
objects using the format of your choice (JSON, protobuf, etc.).

You can for instance use `convertAndSend` and `subscribe` overloads with serializers like this:

```kotlin
import org.hildan.krossbow.stomp.*
import org.hildan.krossbow.stomp.conversions.kxserialization.*

@Serializable
data class Person(val name: String, val age: Int)
@Serializable
data class MyMessage(val timestamp: Long, val author: String, val content: String)

val session = StompClient().connect(url)
val jsonStompSession = session.withJsonConversions() // adds convenience methods for kotlinx.serialization's conversions

jsonStompSession.use { s ->
    s.convertAndSend("/some/destination", Person("Bob", 42), Person.serializer()) 

    // overloads without explicit serializers exist, but should be avoided if you also target JavaScript
    val messages: Flow<MyMessage> = s.subscribe("/some/topic/destination", MyMessage.serializer())

    messages.collect { msg ->
        println("Received message from ${msg.author}: ${msg.content}")
    }
}
```

## Dependency

You will need to declare the `krossbow-stomp-kxserialization` module dependency to add these capabilities (you don't 
need the core module anymore as it is transitively brought by this one):

```
implementation("org.hildan.krossbow:krossbow-stomp-kxserialization:$krossbowVersion")
```

### Peer dependencies

To avoid adding unnecessary runtime dependencies on consumers of Krossbow, `kotlinx-serialization-json` is not
 transitively brought by `krossbow-stomp-serialization` (since Krossbow 0.42.0, using Kotlinx Serialization 1.0.0-RC2).
 
You need to add yourself the relevant Kotlinx Serialization dependency corresponding to the format you want to use.
In the case of JSON, that would be `kotlinx-serialization-json`:

```
implementation("org.hildan.krossbow:krossbow-stomp-kxserialization:$krossbowVersion")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
```

See the [Kotlinx Serialization doc](https://github.com/Kotlin/kotlinx.serialization#dependency-on-the-json-library)
for more information about this.
