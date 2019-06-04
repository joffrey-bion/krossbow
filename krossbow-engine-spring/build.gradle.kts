plugins {
    kotlin("jvm")
    id("org.springframework.boot") version "2.1.5.RELEASE"
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":krossbow-engine-api"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")

    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-messaging")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}
