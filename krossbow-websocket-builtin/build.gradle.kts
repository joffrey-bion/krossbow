plugins {
    id("krossbow-multiplatform")
    id("krossbow-publish")
    id("websocket-test-server")
}

description = "Multiplatform implementation of Krossbow's WebSocket API adapting the platforms' built-in " +
    "implementations (JS browser's WebSocket, JDK11 client on JVM, NSURLSession on Apple targets)."

kotlin {
    jvm()
    js {
        browser()
    }
    appleTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.io.core)
                implementation(projects.krossbowIo)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketTest)
                implementation(kotlin("test"))
            }
        }
    }
}

dokkaExternalDocLink("https://kotlinlang.org/api/kotlinx.coroutines/")
