import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    `kotlin-maven-central-publish`
}

description = "Multiplatform implementation of Krossbow's WebSocket API using Ktor's web sockets."

kotlin {
    jvm()
    jsWithBigTimeouts()
    nativeTargets()

    // On Linux and macOS, we can cross-compile the Windows test executable, but the linker needs mingw64's libcurl.
    // That's why its DLL archive is checked-in and referenced here. It has been taken from a local msys64 installation.
    // Note that, this way, even Windows hosts don't need to install msys2 and libcurl.
    targets.named<KotlinNativeTarget>("mingwX64") {
        binaries["debugTest"].linkerOpts("-L${projectDir.resolve("cygwin-lib")}", "-v")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.ktor2.client.websockets)
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
                implementation(libs.ktor2.client.java)
                implementation(libs.ktor2.client.okhttp)
                implementation(libs.slf4j.simple)
            }
        }

        val linuxX64Test by getting {
            dependencies {
                implementation(libs.ktor2.client.cio)
            }
        }
        val mingwX64Test by getting {
            dependencies {
                implementation(libs.ktor2.client.curl)
            }
        }
        val darwinTest by getting {
            dependencies {
                implementation(libs.ktor2.client.darwin)
            }
        }
    }
}

dokkaExternalDocLink(
    docsUrl = "https://api.ktor.io/ktor-client/",
    packageListUrl = "https://api.ktor.io/package-list",
)
