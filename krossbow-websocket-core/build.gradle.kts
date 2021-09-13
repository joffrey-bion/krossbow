plugins {
    kotlin("multiplatform")
}

description = "WebSocket client API used by the Krossbow STOMP client, with default JS and JVM implementations."

kotlin {
    jvm()
    js {
        useCommonJs()
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
        browser {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
    }
    ios()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
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
