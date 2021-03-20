plugins {
    kotlin("multiplatform")
}

description = "WebSocket client API used by the Krossbow STOMP client, with default JS and JVM implementations."

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
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
                implementation("org.jetbrains.kotlinx:kotlinx-io:${Versions.kotlinxIO}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":krossbow-websocket-test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.coroutines}")
                implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:${Versions.kotlinxIO}")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("com.pusher:java-websocket:1.4.1")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-io-js:${Versions.kotlinxIO}")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
