plugins {
    id("krossbow-multiplatform")
    alias(libs.plugins.kotlin.serialization)
    id("websocket-test-server") // just to test that the server config is available on all target platforms
}

description = "Test utilities for Krossbow WebSocket adapter implementations."

kotlin {
    allTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.turbine)

                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                // for the test server
                implementation(libs.java.websocket)
                implementation(kotlin("test-junit"))
            }
        }
    }
}
