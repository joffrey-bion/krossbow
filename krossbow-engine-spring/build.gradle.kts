plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":krossbow-engine-api"))
    implementation("org.springframework:spring-messaging:4.3.8.RELEASE")
    implementation("org.springframework:spring-websocket:4.3.8.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}
