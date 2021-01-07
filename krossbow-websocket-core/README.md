# Krossbow Web Socket Core

This is a core component of Krossbow, which defines a standard web socket API abstraction.

Different web socket implementations can be used in Krossbow as long as they match the interfaces defined here.

This core module already adapts some built-in implementations on each platform to this common interface:

- JS: the browser's native [WebSocket API](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API)
- JVM: the built-in JDK11 asynchronous
[`java.net.http.WebSocket`](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/WebSocket.html) 

Note that these built-in implementations don't bring in any transitive dependencies.
The ones that do need dependencies are part of different modules, so that they can be included separately only if
needed.
