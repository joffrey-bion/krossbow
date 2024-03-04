plugins {
    id("krossbow-multiplatform")
    alias(libs.plugins.kotlin.serialization)
    id("krossbow-publish")
}

description = "An extension of Krossbow STOMP client using Kotlinx Serialization for message conversions"

kotlin {
    allTargets()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowStompCore)
                api(libs.kotlinx.serialization.core)
                implementation(projects.krossbowIo)
            }
        }
    }
}

dokkaExternalDocLink("https://kotlin.github.io/kotlinx.serialization/")
