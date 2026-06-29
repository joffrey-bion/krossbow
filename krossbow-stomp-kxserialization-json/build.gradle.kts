plugins {
    id("krossbow-multiplatform")
    alias(libs.plugins.kotlin.serialization)
    id("krossbow-publish")
}

description = "An extension of Krossbow STOMP client using Kotlinx Serialization's JSON format for message conversions"

kotlin {
    allTargets()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.krossbowStompKxserialization)
                api(libs.kotlinx.serialization.json)
            }
        }
    }
}

dokkaExternalDocLink("https://kotlin.github.io/kotlinx.serialization/")
