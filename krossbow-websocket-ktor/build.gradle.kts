plugins {
    kotlin("multiplatform")
}

description = "Multiplatform implementation of Krossbow's WebSocket API using Ktor's web sockets."

kotlin {
    jvm()
    js {
        useCommonJs() // required for SockJS top-level declarations usage
        nodejs()
        browser()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.ktor.client.websockets)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.atomicfu)
            }
        }
        val jvmTest by getting {
            dependencies {
                api(libs.ktor.client.okhttp)
            }
        }
        val jsTest by getting {
            dependencies {
                api(libs.ktor.client.js)
            }
        }
    }
}
