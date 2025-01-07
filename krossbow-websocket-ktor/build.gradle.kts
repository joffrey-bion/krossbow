plugins {
    id("krossbow-multiplatform")
    id("krossbow-publish")
    alias(libs.plugins.kotlin.atomicfu)
    id("websocket-test-server")
}

description = "Multiplatform implementation of Krossbow's WebSocket API using Ktor's web sockets."

kotlin {
    ktorTargets()

    sourceSets {
        all {
            languageSettings.optIn("org.hildan.krossbow.io.InternalKrossbowIoApi")
        }
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.ktor.client.websockets)
                implementation(projects.krossbowIo)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(projects.krossbowWebsocketTest)
            }
        }
        val cioSupportTest by creating {
            dependsOn(commonTest)
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        val jvmTest by getting {
            dependsOn(cioSupportTest)
            dependencies {
                implementation(libs.ktor.client.java)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.slf4j.simple)
            }
        }
        val linuxX64Test by getting {
            dependsOn(cioSupportTest)
        }
        val mingwX64Test by getting {
            dependencies {
                implementation(libs.ktor.client.winhttp)
            }
        }
        val appleTest by getting {
            dependsOn(cioSupportTest)
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
