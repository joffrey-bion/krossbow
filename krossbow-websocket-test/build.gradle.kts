plugins {
    id("krossbow-multiplatform")
    alias(libs.plugins.kotlin.serialization)
    id("websocket-test-server") // just to test that the server config is available on all target platforms
}

description = "Test utilities for Krossbow WebSocket adapter implementations."

kotlin {
    allTargets()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)

                implementation(kotlin("test"))
            }
        }
        jvmMain {
            dependencies {
                // for the test server
                implementation(libs.java.websocket)
                implementation(kotlin("test-junit"))
            }
        }
    }
}
