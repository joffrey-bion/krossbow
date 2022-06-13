plugins {
    kotlin("multiplatform")
    `kotlin-maven-central-publish`
}

description = "A Kotlin multiplatform STOMP client relying on built-in websocket implementations on the supported " +
    "platforms"

kotlin {
    jvm()
    js(BOTH) {
        browser()
    }
    darwinTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowStompCore)
                implementation(projects.krossbowWebsocketBuiltin)
            }
        }
    }
}
