# Krossbow Web Socket OkHttp

This module provides the `OkHttpWebSocketClient`, a JVM implementation of the general Web Socket interface 
defined by `krossbow-websocket-core`, adapting [OkHttp's `WebSocket`](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-web-socket/).

This makes it a convenient choice when already using OkHttp as an HTTP client, which is quite popular on Android.

## Dependency

You will need to declare the `krossbow-websocket-okhttp` module dependency to use the `OkHttpWebSocketClient`:

```
implementation("org.hildan.krossbow:krossbow-websocket-okhttp:$krossbowVersion")
```
