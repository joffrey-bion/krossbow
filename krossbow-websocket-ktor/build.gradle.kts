plugins {
    kotlin("multiplatform")
}

description = "Multiplatform implementation of Krossbow's WebSocket API using Ktor's web sockets."

val ktorVersion = "1.4.0"

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
                api("io.ktor:ktor-client-websockets:$ktorVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("org.jetbrains.kotlinx:atomicfu:${Versions.atomicFu}")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
                implementation("io.ktor:ktor-client-js:$ktorVersion")
            }
        }
    }
}
