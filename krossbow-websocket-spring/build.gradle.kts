plugins {
    id("krossbow-jvm")
    id("krossbow-publish")
    id("websocket-test-server")
}

description = "A Krossbow adapter for Spring's default WebSocket client and SockJS client"

dependencies {
    api(projects.krossbowWebsocketCore)

    // For Spring's WebSocket clients
    api(libs.spring.websocket)

    implementation(projects.krossbowIo)
    implementation(libs.kotlinx.io.core)

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
