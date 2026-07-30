# STOMP with Jackson

The `krossbow-stomp-jackson` module is a JVM-only extension of `krossbow-stomp-core` that provides new APIs to
send and receive properly typed classes, and automatically convert them to/from the JSON bodies of STOMP frames
by leveraging [Jackson](https://github.com/FasterXML/jackson) and 
[jackson-module-kotlin](https://github.com/FasterXML/jackson-module-kotlin).

The main addition is the extension function `StompSession.withJackson()`, which turns your `StompSession`
into a `TypedStompSession`.
This new session type has additional methods that use Jackson to convert your objects into JSON and back:

```kotlin
StompClient(WebSocketClient.builtIn()).connect(url).withJackson().use { session ->
    session.convertAndSend("/some/destination", Person("Bob", 42)) 

    val messages: Flow<MyMessage> = session.subscribe<MyMessage>("/some/topic/destination")
    val firstMessage: MyMessage = messages.first()

    println("Received: $firstMessage")
}
```

## Using a custom `ObjectMapper`

Jackson is highly configurable, and it's often useful to configure the `ObjectMapper` manually.

The `withJackson()` method takes an optional `ObjectMapper` parameter, so you can configure it as you please:

```kotlin
val customObjectMapper: ObjectMapper = jacksonObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)

val client = StompClient(WebSocketClient.builtIn()).connect(url)
val session = client.withJackson(customObjectMapper)
```

## Dependency

To use Jackson conversions, add `krossbow-stomp-jackson` to your Gradle dependencies
(`krossbow-stomp-core` is unnecessary because it's transitively brought by this one):

```kotlin
implementation("org.hildan.krossbow:krossbow-stomp-jackson:{{ git.short_tag }}")
```

This dependency transitively brings Jackson {{ versions.jackson }} with the [Kotlin module](https://github.com/FasterXML/jackson-module-kotlin).
