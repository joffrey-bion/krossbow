plugins {
    kotlin("multiplatform")
    id("krossbow-publish")
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
            }
        }
        val jvmMain by getting {
            dependencies {
                api(projects.krossbowWebsocketSpring)
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
