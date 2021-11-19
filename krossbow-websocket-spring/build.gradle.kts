plugins {
    kotlin("jvm")
}

description = "A Krossbow adapter for Spring's default WebSocket client and SockJS client"

dependencies {
    api(projects.krossbowWebsocketCore)

    implementation(libs.kotlinx.coroutines.jdk8)

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

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["kotlin"])
            artifact(sourcesJar)
        }
    }
}
