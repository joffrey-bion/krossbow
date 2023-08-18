plugins {
    kotlin("jvm")
    id("krossbow-publish")
}

description = "A Krossbow adapter for Spring's default WebSocket client and SockJS client"

dependencies {
    api(projects.krossbowWebsocketCore)

    // For Spring's WebSocket clients
    api(libs.spring.websocket)

    testImplementation(kotlin("test"))
    testImplementation(projects.krossbowWebsocketTest)
    testImplementation(libs.slf4j.simple)

    // JSR 356 - Java API for WebSocket (reference implementation)
    // Low-level implementation required by Spring's client (javax.websocket.*)
    testImplementation(libs.tyrusStandaloneClient)
    // Implementation of Jetty client (for jetty tests)
    testImplementation(libs.jettyWebsocketCient)
}

dokkaExternalDocLink("https://javadoc.io/doc/org.springframework/spring-websocket/${libs.versions.spring.get()}/")
