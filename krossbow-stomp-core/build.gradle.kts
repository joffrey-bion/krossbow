import java.net.URL

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

description = "A Kotlin multiplatform STOMP client with JVM, Browser, and NodeJS support"

val coroutinesVersion = "1.3.3"
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$coroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-io:$kotlinxIOVersion")
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
                api(project(":krossbow-websocket-spring"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:$kotlinxIOVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation(project(":krossbow-stomp-jackson"))
                implementation("uk.org.lidalia:slf4j-test:1.1.0")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-io-js:$kotlinxIOVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
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
            externalDocumentationLink {
                url = URL("file://${project(":krossbow-websocket-api").buildDir}/dokka/krossbow-websocket-api/")
                packageListUrl = URL(url, "package-list")
            }
        }
        val jvm by creating {
            externalDocumentationLink {
                url = URL("file://${project(":krossbow-websocket-spring").buildDir}/dokka/krossbow-websocket-spring/")
                packageListUrl = URL(url, "package-list")
            }
        }
    }
}
