plugins {
    id("krossbow-multiplatform")
    id("krossbow-publish")
    alias(libs.plugins.kotlin.atomicfu)
    id("websocket-test-server")
}

description = "Multiplatform implementation of Krossbow's WebSocket API using Ktor's web sockets."

kotlin {
    ktor3Targets()

    sourceSets {
        all {
            languageSettings.optIn("org.hildan.krossbow.io.InternalKrossbowIoApi")
        }
        commonMain {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.ktor.client.websockets)
                implementation(projects.krossbowIo)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(projects.krossbowWebsocketTest)
            }
        }
        val cioSupportTest = create("cioSupportTest") {
            dependsOn(commonTest.get())
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        jsMain {
            dependencies {
                // workaround for https://youtrack.jetbrains.com/issue/KT-57235
                implementation(libs.kotlinx.atomicfu.runtime)
            }
        }
        jvmTest {
            dependsOn(cioSupportTest)
            dependencies {
                implementation(libs.ktor.client.java)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.slf4j.simple)
            }
        }
        linuxX64Test {
            dependsOn(cioSupportTest)
        }
        mingwX64Test {
            dependencies {
                implementation(libs.ktor.client.winhttp)
            }
        }
        appleTest {
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
