import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    implementation(project(":websocket-test-server"))
}

kotlin {
    compilerOptions {
        // this is to match the embedded Kotlin version when used in Gradle build scripts
        languageVersion.set(KotlinVersion.KOTLIN_1_9)
        apiVersion.set(KotlinVersion.KOTLIN_1_9)
    }
}
