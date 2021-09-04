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
    ios()

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                implementation(libs.okio.multiplatform)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketTest)
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(libs.kotlinx.coroutines.jdk8)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("com.pusher:java-websocket:1.4.1")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(npm("isomorphic-ws", "4.0.1"))
                implementation(npm("ws", "8.2.1"))
            }
        }
    }
}
