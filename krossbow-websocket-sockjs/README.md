# Krossbow Web Socket SockJS

This is a multiplatform implementation of the general Web Socket interface defined by `krossbow-websocket-core`.
This implementation uses SockJS clients, which bring in transitive dependencies on each platform.

**Note that using a SockJS client require a SockJS server.**

To use this client, just call `SockJSClient()` and the relevant platform-specific client will be instantiated for you.

The backing implementations on each platform are:

- JS (browser and NodeJS): the [`sockjs-client`](https://github.com/sockjs/sockjs-client) library
- JVM: Spring's Web Socket client (with SockJS enabled), through `krossbow-websocket-spring`

## Dependency

You will need to declare the `krossbow-websocket-sockjs` module dependency:

```
implementation("org.hildan.krossbow:krossbow-websocket-sockjs:$krossbowVersion")
```
