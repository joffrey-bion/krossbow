plugins {
    id("krossbow-multiplatform-all")
    id("krossbow-publish")
    alias(libs.plugins.kotlin.atomicfu)
}

description = "Multiplatform implementation of Krossbow's WebSocket API using Ktor's web sockets."

kotlin {
    jsWithBigTimeouts()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.ktor.client.websockets)
                api(libs.kotlinx.atomicfu)
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
        val darwinTest by getting {
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
