plugins {
    kotlin("multiplatform")
    `kotlin-maven-central-publish`
}

description = "WebSocket client API used by the Krossbow STOMP client"

kotlin {
    jvm()
    jsWithBigTimeouts()
    setupNativeTargets()

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
        setupNativeSourceSets()
    }
}
