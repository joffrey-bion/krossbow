import org.jetbrains.kotlin.gradle.*

plugins {
    id("krossbow-multiplatform")
    id("krossbow-publish")
}

description = "A Kotlin multiplatform STOMP client based on a generic web socket API"

kotlin {
    allTargets()
    sourceSets {
        all {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes") // for charsets
            }
            languageSettings.optIn("org.hildan.krossbow.io.InternalKrossbowIoApi")
            languageSettings.optIn("org.hildan.krossbow.stomp.headers.ExperimentalHeadersApi")
        }
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.kotlinx.io.bytestring)
                implementation(projects.krossbowIo)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.io.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketTest)
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.slf4j.test)
            }
        }
    }
}
