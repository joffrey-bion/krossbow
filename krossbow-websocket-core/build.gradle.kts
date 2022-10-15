plugins {
    kotlin("multiplatform")
    id("krossbow.kotlin-publishing-conventions")
}

description = "WebSocket client API used by the Krossbow STOMP client"

kotlin {
    jvm()
    jsTargets()
    nativeTargets()

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
