plugins {
    kotlin("multiplatform")
    id("krossbow-publish")
    id("websocket-test-server")
}

description = "Multiplatform SockJS implementation of Krossbow's WebSocket API."

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(projects.krossbowWebsocketTest)
            }
        }
        val jvmMain by getting {
            dependencies {
                api(projects.krossbowWebsocketSpring)
            }
        }
        val jvmTest by getting {
            dependencies {
                // JSR 356 - Java API for WebSocket (reference implementation)
                // Low-level implementation required by Spring's client (jakarta.websocket.*)
                implementation(libs.tyrusStandaloneClient)
            }
        }
        val jsMain by getting {
            dependencies {
                api(projects.krossbowWebsocketBuiltin)
                implementation(npm("sockjs-client", libs.versions.npm.sockjs.client.get()))
            }
        }
    }
}
