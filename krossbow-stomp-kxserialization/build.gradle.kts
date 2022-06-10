plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `kotlin-maven-central-publish`
}

description = "An extension of Krossbow STOMP client using Kotlinx Serialization for message conversions"

kotlin {
    jvm()
    js(BOTH) {
        nodejs()
        browser()
    }
    setupNativeTargets()

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
