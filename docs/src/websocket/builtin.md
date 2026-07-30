# Krossbow Web Socket Built-in

The `krossbow-websocket-builtin` module defines implementations of the `WebSocketClient` interface by adapting
clients that already exist in each platform (without 3rd-party dependencies).

For convenience in `common` source sets, this module also provides the
[builtIn()](../kdoc/krossbow-websocket-builtin/org.hildan.krossbow.websocket.builtin/built-in.html)
factory method, which returns the built-in implementation of the current platform.

## JVM

On the JVM target, the
[Jdk11WebSocketClient](../kdoc/krossbow-websocket-builtin/org.hildan.krossbow.websocket.jdk/-jdk11-web-socket-client/index.html)
adapts the built-in
[HttpClient](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.html)
provided in the JRE since Java 11, and its
[WebSocket](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/WebSocket.html) interface.

!!! warning "Android not supported"
    This adapter requires Java 11's `HttpClient` which is not available on Android.
    If you need to support Android, please use the [OkHttp](./okhttp.md) or [Ktor](./ktor.md) adapter instead.

## JavaScript

On the JS target, the 
[BrowserWebSocketClient](../kdoc/krossbow-websocket-builtin/org.hildan.krossbow.websocket.js/-browser-web-socket-client/index.html)
adapts the browser's built-in
[WebSocket](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket) directly.

Also, the `JsWebSocketClientAdapter` allows to adapt anything that looks like the browser's `WebSocket`.

## Darwin (macOS, iOS, tvOS, watchOS)

On all Darwin targets, the 
[DarwinWebSocketClient](../kdoc/krossbow-websocket-builtin/org.hildan.krossbow.websocket.darwin/-darwin-web-socket-client/index.html)
adapts the Foundation framework's
[NSURLSessionWebSocketTask](https://developer.apple.com/documentation/foundation/nsurlsessionwebsockettask).

## Dependency information

To use the built-in web socket clients, add the following to your `build.gradle`:

```kotlin
implementation("org.hildan.krossbow:krossbow-websocket-builtin:{{ git.short_tag }}")
```
