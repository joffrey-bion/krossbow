plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":krossbow-engine-api"))

    api("org.slf4j:slf4j-api:1.7.26")

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect")) // required by jackson-module-kotlin

    // For Spring's STOMP client
    val springVersion = "5.1.7.RELEASE"
    implementation("org.springframework:spring-websocket:$springVersion")
    implementation("org.springframework:spring-messaging:$springVersion")

    // JSR 356 - Java API for WebSocket (reference implementation)
    // Low-level mplementation required by Spring's client
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client-jdk:1.15")

    val jacksonVersion = "2.9.9"
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("uk.org.lidalia:slf4j-test:1.1.0")
}
