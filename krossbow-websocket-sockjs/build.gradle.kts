plugins {
    kotlin("multiplatform")
}

description = "Multiplatform SockJS implementation of Krossbow's WebSocket API."

kotlin {
    jvm()
    js {
        useCommonJs() // required for SockJS top-level declarations usage
        nodejs()
        browser()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":krossbow-websocket-core"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(project(":krossbow-websocket-spring"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(npm("sockjs-client", "1.4.0"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
