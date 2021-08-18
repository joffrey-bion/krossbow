plugins {
    kotlin("multiplatform")
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
    setupNativeTargets()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.okio)
                implementation(libs.uuid)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketTest)
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("uk.org.lidalia:slf4j-test:1.1.0")
            }
        }
        setupNativeSourceSets()
    }
}
