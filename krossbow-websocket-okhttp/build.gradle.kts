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

// using "latest" because not all versions are published (e.g. 4.x.x are not published)
dokkaExternalDocLink("https://javadoc.io/doc/com.squareup.okhttp3/okhttp/latest/")
