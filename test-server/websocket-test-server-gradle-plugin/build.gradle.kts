plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    implementation(project(":websocket-test-server"))
}
