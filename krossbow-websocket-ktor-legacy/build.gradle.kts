plugins {
    id("krossbow-multiplatform")
    id("krossbow-publish")
    alias(libs.plugins.kotlin.atomicfu)
    id("websocket-test-server")
}

description = "Multiplatform implementation of Krossbow's WebSocket API using Ktor's web sockets."

kotlin {
    ktor2Targets()

    sourceSets {
        all {
            languageSettings.optIn("org.hildan.krossbow.io.InternalKrossbowIoApi")
        }
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.ktorLegacy.client.websockets)
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
                implementation(libs.ktorLegacy.client.cio)
            }
        }
        val jsMain by getting {
            dependencies {
                // workaround for https://youtrack.jetbrains.com/issue/KT-57235
                implementation(libs.kotlinx.atomicfu.runtime)
            }
        }
        val jvmTest by getting {
            dependsOn(cioSupportTest)
            dependencies {
                implementation(libs.ktorLegacy.client.java)
                implementation(libs.ktorLegacy.client.okhttp)
                implementation(libs.slf4j.simple)
            }
        }
        val linuxX64Test by getting {
            dependsOn(cioSupportTest)
        }
        val mingwX64Test by getting {
            dependencies {
                implementation(libs.ktorLegacy.client.winhttp)
            }
        }
        val appleTest by getting {
            dependsOn(cioSupportTest)
            dependencies {
                implementation(libs.ktorLegacy.client.darwin)
            }
        }
    }
}

dokkaExternalDocLink(
    docsUrl = "https://api.ktor.io/ktor-client/",
    packageListUrl = "https://api.ktor.io/package-list",
)
