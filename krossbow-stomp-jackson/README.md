# Krossbow STOMP Jackson

This is a JVM-only extension of `krossbow-stomp-core` that provides convenient JSON conversions using 
[Jackson](https://github.com/FasterXML/jackson).

The main addition is the extension function `StompSession.withJacksonConversions()`, which turns your `StompSession`
into a `StompSessionWithClassConversions`.
This new session type has additional methods that use reflection to convert your objects into JSON and back:
 
```kotlin
StompClient().connect(url).withJacksonConversions().use {
    convertAndSend("/some/destination", Person("Bob", 42)) 

    val subscription: Flow<MyMessage> = subscribe<MyMessage>("/some/topic/destination")
    val firstMessage: MyMessage = subscription.first()

    println("Received: $firstMessage")
}
```

## Dependency

You will need to declare the `krossbow-stomp-jackson` module dependency to add these capabilities (you don't need the
core module anymore as it is transitively brought by this one):

```
implementation("org.hildan.krossbow:krossbow-stomp-jackson:$krossbowVersion")
```
