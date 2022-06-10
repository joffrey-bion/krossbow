plugins {
    kotlin("multiplatform")
    `kotlin-maven-central-publish`
}

description = "WebSocket client API used by the Krossbow STOMP client, with default JS and JVM implementations."

kotlin {
    jvm()
    jsWithBigTimeouts()
    setupNativeTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                implementation(libs.okio)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketTest)
                implementation(libs.kotlinx.coroutines.test)
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
                implementation(npm("isomorphic-ws", libs.versions.npm.isomorphic.ws.get()))
                implementation(npm("ws", libs.versions.npm.ws.get()))
            }
        }
        setupNativeSourceSets()
    }
}
