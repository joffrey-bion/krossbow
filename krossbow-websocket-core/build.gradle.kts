plugins {
    id("krossbow-multiplatform")
    id("krossbow-publish")
}

description = "WebSocket client API used by the Krossbow STOMP client"

kotlin {
    allTargets()
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.io.bytestring)
                implementation(libs.kotlinx.io.core)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.krossbowWebsocketTest)
                implementation(libs.kotlinx.coroutines.test)
                implementation(kotlin("test"))
            }
        }
    }
}

dokkaExternalDocLink("https://kotlinlang.org/api/kotlinx.coroutines/")
