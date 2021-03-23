plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

description = "A Kotlin multiplatform STOMP client with JVM, Browser, and NodeJS support"

kotlin {
    jvm()
    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
        browser {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
    }
    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
        val commonMain by getting {
            dependencies {
                api(project(":krossbow-websocket-core"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
                implementation("com.squareup.okio:okio-multiplatform:${Versions.okio}")
                implementation("com.benasher44:uuid:0.1.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                // For kotlinx-coroutines-test
                implementation("org.jetbrains.kotlinx:atomicfu:${Versions.atomicFu}")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("uk.org.lidalia:slf4j-test:1.1.0")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
