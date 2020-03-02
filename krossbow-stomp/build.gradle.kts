import java.net.URL

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

description = "A Kotlin multiplatform STOMP client with JVM and JS support"

val coroutinesVersion = "1.3.1"
val jacksonVersion = "2.9.9"
val serializationVersion = "0.14.0"
val kotlinxIOVersion = "0.1.16"

kotlin {
    jvm()
    js {
        nodejs()
        browser()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                api(project(":krossbow-websocket-api"))
                implementation("org.jetbrains.kotlinx:kotlinx-io:$kotlinxIOVersion")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$coroutinesVersion")
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
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
                implementation(kotlin("stdlib-jdk8"))
                implementation(project(":krossbow-websocket-spring"))
                implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:$kotlinxIOVersion")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
                implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("uk.org.lidalia:slf4j-test:1.1.0")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-io-js:$kotlinxIOVersion")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
                implementation(npm("text-encoding", "0.7.0")) // seems required by kotlinx-io-js
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
    dependsOn(
        ":krossbow-websocket-api:dokka",
        ":krossbow-websocket-spring:dokka"
    )
    //    outputFormat = "javadoc"
    multiplatform {
        val global by creating {
//            externalDocumentationLink {
//                url = URL("file://${project(":krossbow-websocket-api").buildDir}/dokka/krossbow-websocket-api/")
//                packageListUrl = URL(url, "package-list")
//            }
        }
        val jvm by creating {
            externalDocumentationLink {
                url = URL("file://${project(":krossbow-websocket-spring").buildDir}/dokka/krossbow-websocket-spring/")
                packageListUrl = URL(url, "package-list")
            }
        }
    }
}
