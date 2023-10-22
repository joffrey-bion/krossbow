plugins {
    id("krossbow-multiplatform")
    id("krossbow-publish")
}

description = "WebSocket client API used by the Krossbow STOMP client"

kotlin {
    allTargets()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                implementation(libs.okio)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketTest)
                implementation(libs.kotlinx.coroutines.test)
                implementation(kotlin("test"))
            }
        }
    }
}

dokkaExternalDocLink("https://kotlinlang.org/api/kotlinx.coroutines/")
