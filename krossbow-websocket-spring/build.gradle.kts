plugins {
    kotlin("jvm")
}

description = "A Krossbow adapter for Spring's default WebSocket client and SockJS client"

dependencies {
    api(projects.krossbowWebsocketCore)

    api("org.slf4j:slf4j-api:1.7.26")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${libs.versions.coroutines}")

    // For Spring's WebSocket clients
    api("org.springframework:spring-websocket:5.3.6")

    // JSR 356 - Java API for WebSocket (reference implementation)
    // Low-level implementation required by Spring's client (javax.websocket.*)
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client-jdk:1.17")

    testImplementation(kotlin("test"))
    testImplementation(projects.krossbowWebsocketTest)
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
