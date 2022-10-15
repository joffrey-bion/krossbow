plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("krossbow.kotlin-publishing-conventions")
}

description = "An extension of Krossbow STOMP client using Kotlinx Serialization's JSON format for message conversions"

kotlin {
    jvm()
    jsTargets()
    nativeTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowStompKxserialization)
                api(libs.kotlinx.serialization.json)
            }
        }
    }
}

dokkaExternalDocLink("https://kotlin.github.io/kotlinx.serialization/")
