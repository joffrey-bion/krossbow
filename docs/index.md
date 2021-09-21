Krossbow is a Kotlin multiplatform [STOMP 1.2](https://stomp.github.io/index.html) client with a coroutine-based API.

It is built on a web socket client abstraction, and provides a bunch of adapters for popular web socket clients
(OkHttp, Ktor, Spring, SockJS...).
It also provides out-of-the-box a built-in web socket implementation (without third-party dependencies) for most platforms (see below).

Krossbow can also be used as a multiplatform web socket client without STOMP protocol.

## Features

All of the [STOMP 1.2](https://stomp.github.io/index.html) specification is implemented:

- All STOMP frames, including `ACK`/`NACK` and transactions
- Text and binary bodies
- [Receipts](stomp/advanced-features.md#receipts-suspension) (waiting for `RECEIPT` frame based on receipt header)
- [Heart beats](stomp/advanced-features.md#heart-beats) (keep alive)
- Custom headers where the protocol allows them

Additional features:

- Auto-receipts (automatically adds `receipt` headers to ensure no frame is lost)
- [Built-in body conversions](stomp/basics.md#using-body-conversions) (Kotlinx Serialization or Jackson)
- Possibility to hook [custom body converters](stomp/conversions/custom.md) (for textual or binary bodies)
- Automatic content length header for sent frames

!!! tip "If you find a bug or a feature that's missing compared to the specification, please [open an issue](https://github.com/joffrey-bion/krossbow/issues)."

## Supported targets

Krossbow supports the following targets:

* :white_check_mark: JVM
* :white_check_mark: JS (browser & NodeJS)
* :warning: Android (using JVM artifacts)
* :construction: iOS ([in progress](https://github.com/joffrey-bion/krossbow/issues/126))
* :x: Native Desktop

The desktop Native targets are currently unsupported, due to a lack of native web socket implementations.
Contributions are of course welcome!

!!! info "Android not tested on CI"
    Android 5.0+ (API level 21+) is supported by using JVM artifacts (e.g. OkHttp).
    However, the Android tooling's desugaring is currently not tested as part of the build, so any feedback on this use
    case is more than welcome.
    Please upvote [the corresponding issue](https://github.com/joffrey-bion/krossbow/issues/49) if you'd like to see
    proper CI or special packaging for the Android target.

### Web socket clients target support

Krossbow can use built-in web socket implementations without third-party dependencies on some platforms.
It also provides adapters for third-party implementations which have different platform support.
Here is a summary of the supported platform by module:

| Web Socket Module                   |         Browser        |         NodeJS         |              JVM8+ (blocking)             |   JVM11+ (async)   | Transitive dependencies |
|-------------------------------------|:----------------------:|:----------------------:|:-----------------------------------------:|:------------------:|-------------------------|
| [Core (built-in)](./websocket/core) |   :white_check_mark:   |                        |                                           | :white_check_mark: | None                    |
| [Ktor](./websocket/ktor)            |   :white_check_mark:   |   :white_check_mark:   | :white_check_mark: \*                     | :white_check_mark: | [Ktor](https://ktor.io/clients/websockets.html), and the relevant [Ktor engine(s)](https://ktor.io/clients/http-client/engines.html) |
| [OkHttp](./websocket/okhttp)        |                        |                        | :white_check_mark:                        |                    | [OkHttp](https://square.github.io/okhttp/) |
| [SockJS](./websocket/sockjs)        | :large_orange_diamond: | :large_orange_diamond: |                    :large_orange_diamond: |                    | [sockjs-client](https://github.com/sockjs/sockjs-client) (on JS), [Spring websocket](https://docs.spring.io/spring-framework/docs/5.0.0.BUILD-SNAPSHOT/spring-framework-reference/html/websocket.html) (on JVM) |
| [Spring](./websocket/spring)        |                        |                        | :white_check_mark: :large_orange_diamond: |                    | [Spring websocket](https://docs.spring.io/spring-framework/docs/5.0.0.BUILD-SNAPSHOT/spring-framework-reference/html/websocket.html) |

:white_check_mark: supported with actual web socket transport (RFC6455)

:large_orange_diamond: supported using [SockJS](https://github.com/sockjs/sockjs-client) protocol (requires a SockJS server)

\* backed by blocking I/O when using the OkHttp engine, but can be fully non-blocking with the CIO engine

## Contribute

Don't hesitate to [open GitHub issues](https://github.com/joffrey-bion/krossbow/issues), even to ask questions or discuss a new feature.
Pull-requests are welcome, but please open an issue first so that we can discuss the initial design or fix, which may avoid unnecessary work.
