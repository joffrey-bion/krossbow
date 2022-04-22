Krossbow offers a lot of possibilities so here is a summary of all available artifacts.

All these artifacts are published to Maven Central under the group ID `org.hildan.krossbow` and a common version.

## STOMP artifacts

You should pick only one of the `krossbow-stomp-*` artifacts, depending on whether you need automatic serialization of frame bodies:

| Artifact                                  | Description                                                                                                                                                                                                              |
|-------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| <pre>krossbow-stomp-core</pre>            | The basic multiplatform STOMP client. It implements the STOMP 1.2 protocol on top of the web socket abstraction defined by the `krossbow-websocket-core` module.                                                         |
| <pre>krossbow-stomp-jackson</pre>         | A superset of `krossbow-stomp-core` adding JSON conversion features using Jackson (JVM only)                                                                                                                             |
| <pre>krossbow-stomp-kxserialization</pre> | A superset of `krossbow-stomp-core` adding conversion features using Kotlinx Serialization library (multiplatform). You can leverage the multi-format capabilities of Kotlinx Serialization (JSON, protobuf, CBOR, ...). |

Then add the dependency of your choice to your Gradle build.
For instance, if you intend to use Krossbow with Kotlinx Serialization:

```kotlin
implementation("org.hildan.krossbow:krossbow-stomp-kxserialization:{{ git.tag }}")
```

!!! tip "Don't need STOMP?"
    If you're just interested in the web socket client without STOMP protocol, don't declare a STOMP artifact, but 
    instead choose either:

      * the [built-in Core web socket implementation](./websocket/core.md) (without third-party dependencies)
      * a third party web socket implementation (see below)

## Third party web socket adapters

The STOMP artifacts come with the built-in web socket client for each platform, so no additional dependency is required.
However, if you want a different web socket implementation, you can pick from the other web socket adapters and add the relevant artifact:

| Artifact                                  | Description                                                                                                                                                                      |
|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| <pre>krossbow-websocket-ktor</pre>        | A multiplatform `WebSocketClient` implementation based on Ktor {{ versions.ktor }}'s `HttpClient`.                                                                               |
| <pre>krossbow-websocket-ktor-legacy</pre> | A multiplatform `WebSocketClient` implementation based on Ktor {{ versions.ktorLegacy }}'s `HttpClient`.                                                                         |
| <pre>krossbow-websocket-okhttp</pre>      | a JVM implementation of the web socket API using OkHttp's                                                                                                                        |
| <pre>krossbow-websocket-sockjs</pre>      | a multiplatform `WebSocketClient` implementation for use with SockJS servers. It uses Spring's SockJSClient on JVM, and npm `sockjs-client` for JavaScript (NodeJS and browser). |
| <pre>krossbow-websocket-spring</pre>      | a JVM 8+ implementation of the web socket API using Spring's WebSocketClient. Provides both a normal WebSocket client and a SockJS one.                                          |

!!! warning "Peer dependencies"
    Some Krossbow modules are not opinionated and require some extra third-party peer dependencies.
    Make sure to read the usage section corresponding to the module of your choice for more details.  
