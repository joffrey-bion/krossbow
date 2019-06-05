plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":krossbow-engine-api"))

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect")) // required by jackson-module-kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")

    // For Spring's STOMP client
    val springVersion = "5.1.7.RELEASE"
    implementation("org.springframework:spring-websocket:$springVersion")
    implementation("org.springframework:spring-messaging:$springVersion")

    // JSR 356 - Java API for WebSocket (reference implementation)
    // Low-level mplementation required by Spring's client
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client-jdk:1.15")

    implementation("com.fasterxml.jackson.core:jackson-core:2.9.9")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.9")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}
