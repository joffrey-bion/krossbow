# Krossbow

A Kotlin multiplatform STOMP client over websockets

This project contains the following modules:
- `krossbow-client`: the multiplatform client to use as a STOMP library in common, JVM or JS projects
- `krossbow-engine-api`: the API that engines must conform to in order to be used by the multiplatform client
- `krossbow-engine-spring`: a JVM implementation of the engine API using Spring's WebsocketStompClient as backend
- `krossbow-engine-webstompjs`: a JavaScript implementation of the engine API using 
[webstomp-client](https://github.com/JSteunou/webstomp-client) as backend

**DISCLAIMER** - The project has just started. It is not usable right now

For now, the `krossbow-client` artifact is going to include the spring and webstomp 
engines so that users don't have to specify anything. This doesn't have any drawback for now as there is only 1 
engine implementation per platform. If the need arises to have more implementations on a single, I'll remove the 
engine artifacts from the client artifact, and users will have to include an engine manually.
