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
        commonMain {
            dependencies {
                api(projects.krossbowWebsocketCore)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmMain {
            dependencies {
                api(projects.krossbowWebsocketSpring)
            }
        }
        jsMain {
            dependencies {
                api(projects.krossbowWebsocketBuiltin)
                implementation(npm("sockjs-client", libs.versions.npm.sockjs.client.get()))
            }
        }
    }
}
