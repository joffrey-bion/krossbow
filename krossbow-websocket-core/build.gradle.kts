plugins {
    kotlin("multiplatform")
}

description = "WebSocket client API used by the Krossbow STOMP client, with default JS and JVM implementations."

kotlin {
    jvm()
    js {
        useCommonJs()
        nodejs()
        browser()
    }
    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
                implementation("com.squareup.okio:okio-multiplatform:${Versions.okio}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":krossbow-websocket-test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.coroutines}")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("com.pusher:java-websocket:1.4.1")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
