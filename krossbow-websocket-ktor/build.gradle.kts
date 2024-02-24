plugins {
    id("krossbow-multiplatform")
    id("krossbow-publish")
    alias(libs.plugins.kotlin.atomicfu)
    id("websocket-test-server")
}

description = "Multiplatform implementation of Krossbow's WebSocket API using Ktor's web sockets."

kotlin {
    allTargets()
    jsWithBigTimeouts()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.ktor.client.websockets)
                api(libs.kotlinx.atomicfu)
                implementation(projects.krossbowIo)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(projects.krossbowWebsocketTest)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.ktor.client.java)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.slf4j.simple)
            }
        }

        val linuxX64Test by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        val mingwX64Test by getting {
            dependencies {
                implementation(libs.ktor.client.winhttp)
            }
        }
        val appleMain by getting {
            dependencies {
                compileOnly(libs.ktor.client.darwin) // FIXME remove after investigation
            }
        }
        val appleTest by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

dokkaExternalDocLink(
    docsUrl = "https://api.ktor.io/ktor-client/",
    packageListUrl = "https://api.ktor.io/package-list",
)
