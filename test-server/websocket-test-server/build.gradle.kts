import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.java.websocket)
    implementation(libs.kotlinx.coroutines.core)
}

kotlin {
    compilerOptions {
        // this is to match the embedded Kotlin version when used in Gradle build scripts
        languageVersion.set(KotlinVersion.KOTLIN_1_9)
        apiVersion.set(KotlinVersion.KOTLIN_1_9)
    }
}
