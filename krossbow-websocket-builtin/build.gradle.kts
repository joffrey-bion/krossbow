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
        all {
            languageSettings.optIn("org.hildan.krossbow.io.InternalKrossbowIoApi")
        }
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.kotlinx.coroutines.core)
                implementation(projects.krossbowIo)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketTest)
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlinx.io.core)
            }
        }
    }
}

dokkaExternalDocLink("https://kotlinlang.org/api/kotlinx.coroutines/")
