plugins {
    kotlin("multiplatform")
}

description = "Test utilities for Krossbow WebSocket adapter implementations."

kotlin {
    jvm()
    js {
        useCommonJs()
        nodejs()
        browser()
    }
    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
        val commonMain by getting {
            dependencies {
                api(project(":krossbow-websocket-core"))
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
                implementation("org.jetbrains.kotlinx:kotlinx-io:${Versions.kotlinxIO}")

                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:${Versions.kotlinxIO}")

                // for the test server
                implementation("com.pusher:java-websocket:1.4.1")

                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-io-js:${Versions.kotlinxIO}")

                implementation(kotlin("test-js"))
            }
        }
    }
}
