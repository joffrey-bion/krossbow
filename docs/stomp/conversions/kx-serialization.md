# STOMP with Kotlinx Serialization

[Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) is a multiplatform and multi-format
serialization library provided by the Kotlin team.
It is a popular choice for Kotlin multiplatform libraries especially because of its extensive support of Kotlin features.

The `krossbow-stomp-kxserialization` module is an extension of `krossbow-stomp-core` that provides new APIs to 
send and receive properly typed classes, and automatically convert STOMP frame bodies by leveraging Kotlinx Serialization.

## A note on JSON format

Since Kotlinx Serialization supports multiple formats with different dependencies, you have to add your own dependency
to bring the format of your choice.

However, since JSON is so popular, Krossbow comes with the `krossbow-stomp-kxserialization-json` module, which adds 
dedicated helpers for JSON and the necessary transitive dependency on the JSON format.

## Basic usage

This module brings the following extension functions on `StompSession`:

- [`withBinaryConversions(format: BinaryFormat, mediaType: String)`](../../kdoc/krossbow-stomp-kxserialization/org.hildan.krossbow.stomp.conversions.kxserialization/with-binary-conversions.html)
- [`withTextConversions(format: StringFormat, mediaType: String)`](../../kdoc/krossbow-stomp-kxserialization/org.hildan.krossbow.stomp.conversions.kxserialization/with-text-conversions.html)

!!! tip If you're using JSON, dedicated [withJsonConversions](../../kdoc/krossbow-stomp-kxserialization-json/org.hildan.krossbow.stomp.conversions.kxserialization.json/with-json-conversions.html)
helpers for JSON serialization is provided in the `krossbow-stomp-serialization-json` module.

These helpers turn your `StompSession` into a `StompSessionWithKxSerialization`.
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

## Custom `Json` instance

The `withJsonConversions()` method takes an optional `Json` parameter, so you can configure it as you please:

```kotlin
// custom Json configuration
val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

val session = StompClient().connect(url)
val jsonStompSession = session.withJsonConversions(json)
```

## Dependency

You will need to declare the following Gradle dependency to add these capabilities
(you don't need the core module anymore as it is transitively brought by this one):

```kotlin
implementation("org.hildan.krossbow:krossbow-stomp-kxserialization:{{ git.tag }}")
```

### Extra configuration

Krossbow is format-agnostic, so you also need to add a Kotlinx Serialization dependency for the format you want to use.
In the case of JSON, that would be `kotlinx-serialization-json`:

```kotlin
implementation("org.hildan.krossbow:krossbow-stomp-kxserialization:{{ git.tag }}")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:{{ versions.kotlinxSerialization }}")
```

Note that Kotlinx Serialization also requires a compiler plugin to generate serializers for your `@Serializable` classes.
See the [Kotlinx Serialization doc](https://github.com/Kotlin/kotlinx.serialization#dependency-on-the-json-library)
for more information about this.
