If you want to use your own text conversion, you can implement `TextMessageConverter` without
any additional module, and use `withTextConversions` to wrap your `StompSession` into a
`StompSessionWithClassToTextConversions`.

!!! warning "Limited JS support" 
    Reflection-based conversions are very likely to behave poorly on the JS platform.
    It is usually safer to [rely on Kotlinx Serialization](./kx-serialization.md) for multiplatform conversions.

```kotlin
val myConverter = object : TextMessageConverter {
    override val mimeType: String = "application/json;charset=utf-8"

    override fun <T : Any> convertToString(body: T, bodyType: KClass<T>): String {
        TODO("your own object -> text conversion")
    }

    override fun <T : Any> convertFromString(body: String, bodyType: KClass<T>): T {
        TODO("your own text -> object conversion")
    }
}

StompClient().connect(url).withTextConversions(myConverter).use { session ->
    session.convertAndSend("/some/destination", MyPojo("Custom", 42)) 

    val messages = session.subscribe<MyMessage>("/some/topic/destination")
    val firstMessage: MyMessage = messages.first()

    println("Received: $firstMessage")
}
```
