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
                implementation(kotlin("stdlib-common"))
                api(project(":krossbow-websocket-core"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:${Versions.coroutines}")
                implementation("org.jetbrains.kotlinx:kotlinx-io:${Versions.kotlinxIO}")
                implementation("com.benasher44:uuid:0.1.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":krossbow-stomp-kxserialization"))
                implementation(project(":krossbow-websocket-sockjs"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.coroutines}")
                implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:${Versions.kotlinxIO}")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation(project(":krossbow-stomp-jackson"))
                implementation(project(":krossbow-websocket-spring"))
                implementation("uk.org.lidalia:slf4j-test:1.1.0")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:${Versions.coroutines}")
                implementation("org.jetbrains.kotlinx:kotlinx-io-js:${Versions.kotlinxIO}")

                // Currently required by kotlinx-io-js
                // https://github.com/Kotlin/kotlinx-io/issues/57
                implementation(npm("text-encoding", "0.7.0"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

tasks.dokka {
    dependsOn(":krossbow-websocket-core:dokka")
    multiplatform {
        val global by creating {
            externalDocumentationLink {
                url = relativeDokkaUrl("krossbow-websocket-core")
                packageListUrl = relativeDokkaPackageListUrl("krossbow-websocket-core")
            }
        }
        val jvm by creating {}
        // Dokka disabled for JS because of NPM dependency breaking the generation
        // https://github.com/Kotlin/dokka/issues/537
        // val js by creating {}
    }
}
