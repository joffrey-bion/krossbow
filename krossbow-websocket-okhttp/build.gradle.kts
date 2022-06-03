plugins {
    kotlin("jvm")
    `kotlin-maven-central-publish`
}

description = "A Krossbow adapter for OkHttp's WebSocket client"

dependencies {
    api(projects.krossbowWebsocketCore)
    api(libs.okhttp)
    implementation(libs.kotlinx.coroutines.jdk8)

    testImplementation(kotlin("test"))
    testImplementation(projects.krossbowWebsocketTest)
}
