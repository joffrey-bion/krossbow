plugins {
    id("krossbow-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

description = "Test utilities for Krossbow WebSocket adapter implementations."

kotlin {
    allTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)

                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                // for the test server
                implementation(libs.java.websocket)

                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val nativeMain by getting
        val nonAppleNativeMain by creating {
            dependsOn(nativeMain)
        }
        val linuxX64Main by getting {
            dependsOn(nonAppleNativeMain)
        }
        val mingwX64Main by getting {
            dependsOn(nonAppleNativeMain)
        }
    }
}
