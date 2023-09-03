# STOMP with Kotlinx Serialization

[Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) is a multiplatform and multi-format
serialization library provided by the Kotlin team.
It is a popular choice for Kotlin multiplatform libraries especially because of its extensive support of Kotlin features.

The `krossbow-stomp-kxserialization` module is an extension of `krossbow-stomp-core` that provides new APIs to 
send and receive properly typed classes, and automatically convert STOMP frame bodies by leveraging Kotlinx Serialization.

## Usage with the JSON format

Since Kotlinx Serialization supports multiple formats with different dependencies, you normally have to add your own
dependency to bring the format of your choice.

However, since JSON is so popular, Krossbow comes with the `krossbow-stomp-kxserialization-json` module, which adds 
dedicated helpers for JSON *and* the necessary transitive dependency on the JSON format (see dependencies section below).

This module brings the [withJsonConversions](../../kdoc/krossbow-stomp-kxserialization-json/org.hildan.krossbow.stomp.conversions.kxserialization.json/with-json-conversions.html)
helper to convert your `StompSession` into a `StompSessionWithKxSerialization`:

```kotlin
val session = StompClient(WebSocketClient.builtIn()).connect(url)
val jsonStompSession = session.withJsonConversions()
```

This new session type has the additional `convertAndSend` method and `subscribe` overloads that use Kotlinx
Serialization's serializers to convert your payloads using the format of your choice (in this case, JSON):

```kotlin
@Serializable
data class Person(val name: String, val age: Int)
@Serializable
data class MyMessage(val timestamp: Long, val author: String, val content: String)

jsonStompSession.use { s ->
    s.convertAndSend("/some/destination", Person("Bob", 42), Person.serializer()) 

    // overloads without explicit serializers exist, but should be avoided if you also target JavaScript
    val messages: Flow<MyMessage> = s.subscribe("/some/topic/destination", MyMessage.serializer())

    messages.collect { msg ->
        println("Received message from ${msg.author}: ${msg.content}")
    }
}
```

### Custom `Json` instance

The `withJsonConversions()` method takes an optional `Json` parameter, so you can configure it as you please:

```kotlin
// custom Json configuration
val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

val session = StompClient(WebSocketClient.builtIn()).connect(url)
val jsonStompSession = session.withJsonConversions(json)
```

## Usage with other formats

For other formats than JSON, use the more general `krossbow-stomp-kxserialization` module, and add a dependency on the
Kotlinx Serialization format of your choice (see dependencies section below).

This module brings the following extension functions on `StompSession`:

- [`withBinaryConversions(format: BinaryFormat, mediaType: String)`](../../kdoc/krossbow-stomp-kxserialization/org.hildan.krossbow.stomp.conversions.kxserialization/with-binary-conversions.html)
- [`withTextConversions(format: StringFormat, mediaType: String)`](../../kdoc/krossbow-stomp-kxserialization/org.hildan.krossbow.stomp.conversions.kxserialization/with-text-conversions.html)

These helpers are equivalent to `withJsonConversions`, but more general, and also turn your `StompSession` into a
`StompSessionWithKxSerialization`.
You should provide the media type that you want to set as `content-type` header in the messages you send:

```kotlin
val session = StompClient(WebSocketClient.builtIn()).connect(url)
val jsonStompSession = session.withBinaryConversions(Protobuf.Default, "application/x-protobuf")
```

You can then use `convertAndSend` and `subscribe` the same way as in the JSON section above.

## Dependency

### General case

Krossbow's base Kotlinx Serialization module is format-agnostic, so you need to add both the
`krossbow-stomp-kxserialization` dependency and the Kotlinx Serialization dependency for the format you want to use.
For instance in the case of protobuf, that would be `kotlinx-serialization-protobuf`:

```kotlin
implementation("org.hildan.krossbow:krossbow-stomp-kxserialization:{{ git.tag }}")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:{{ versions.kotlinxSerialization }}")
```

### JSON format

Since JSON is so common, Krossbow provides an all-in-one module with additional helpers for JSON:

```kotlin
implementation("org.hildan.krossbow:krossbow-stomp-kxserialization-json:{{ git.tag }}")
```

This module brings `kotlinx-serialization-json` transitively, so you don't have to add it yourself.

### Additional notes

With this setup, `krossbow-stomp-core` is unnecessary because it's transitively brought by the
`krossbow-stomp-kxserialization` modules.

Note that Kotlinx Serialization also requires a compiler plugin to generate serializers for your `@Serializable` classes.
See the [Kotlinx Serialization doc](https://github.com/Kotlin/kotlinx.serialization#dependency-on-the-json-library)
for more information about this.
