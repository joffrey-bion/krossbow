plugins {
    kotlin("jvm")
}

description = "A Krossbow adapter for Spring's default WebSocket client and SockJS client"

dependencies {
    api(projects.krossbowWebsocketCore)

    implementation(libs.kotlinx.coroutines.jdk8)

    // For Spring's WebSocket clients
    api("org.springframework:spring-websocket:5.3.9")

    // TODO move this to testImplementation and make the users add it?
    // JSR 356 - Java API for WebSocket (reference implementation)
    // Low-level implementation required by Spring's client (javax.websocket.*)
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client-jdk:1.17")

    testImplementation(kotlin("test"))
    testImplementation(projects.krossbowWebsocketTest)
    testImplementation("org.slf4j:slf4j-simple:1.7.26")

    // Implementation of Jetty client (for jetty tests)
    testImplementation("org.eclipse.jetty.websocket:websocket-client:9.4.43.v20210629")
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
