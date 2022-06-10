plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `kotlin-maven-central-publish`
}

description = "An extension of Krossbow STOMP client using Kotlinx Serialization's JSON format for message conversions"

kotlin {
    jvm()
    js(BOTH) {
        nodejs()
        browser()
    }
    setupNativeTargets()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        val commonMain by getting {
            dependencies {
                api(projects.krossbowStompKxserialization)
                api(libs.kotlinx.serialization.json)
            }
        }
    }
}

dokkaExternalDocLink("https://kotlin.github.io/kotlinx.serialization/")
