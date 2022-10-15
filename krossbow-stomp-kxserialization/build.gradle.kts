plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("krossbow.kotlin-publishing-conventions")
}

description = "An extension of Krossbow STOMP client using Kotlinx Serialization for message conversions"

kotlin {
    jvm()
    jsTargets()
    nativeTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowStompCore)
                api(libs.kotlinx.serialization.core)
            }
        }
    }
}

dokkaExternalDocLink("https://kotlin.github.io/kotlinx.serialization/")
