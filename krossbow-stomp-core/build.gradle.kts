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
                api(projects.krossbowWebsocketCore)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.coroutines.get()}")
                implementation("com.squareup.okio:okio-multiplatform:${libs.versions.okio.get()}")
                implementation("com.benasher44:uuid:0.3.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketTest)
                implementation(kotlin("test"))
                // For kotlinx-coroutines-test
                implementation("org.jetbrains.kotlinx:atomicfu:${libs.versions.atomicFu.get()}")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("uk.org.lidalia:slf4j-test:1.1.0")
            }
        }
    }
}
