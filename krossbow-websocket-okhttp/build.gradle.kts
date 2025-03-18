plugins {
    id("krossbow-jvm")
    id("krossbow-publish")
    id("websocket-test-server")
}

description = "A Krossbow adapter for OkHttp's WebSocket client"

kotlin {
    compilerOptions.optIn.add("org.hildan.krossbow.io.InternalKrossbowIoApi")
}

dependencies {
    api(projects.krossbowWebsocketCore)
    api(libs.okhttp)
    implementation(projects.krossbowIo)
    implementation(libs.kotlinx.io.okio)

    testImplementation(kotlin("test"))
    testImplementation(projects.krossbowWebsocketTest)
}

// using "latest" because not all versions are published (e.g. 4.x.x are not published)
dokkaExternalDocLink("https://javadoc.io/doc/com.squareup.okhttp3/okhttp/latest/")
