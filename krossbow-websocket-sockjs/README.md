# Krossbow Web Socket SockJS

This is a multiplatform implementation of the general Web Socket interface defined by `krossbow-websocket-core`.
This implementation uses SockJS clients, which bring in transitive dependencies on each platform.

**Note that using a SockJS client require a SockJS server.**

The backing implementations on each platform are:

- JS: the [`sockjs-client`](https://github.com/sockjs/sockjs-client) library
- JVM: Spring's Web Socket client (with SockJS enabled), through `krossbow-websocket-spring`
