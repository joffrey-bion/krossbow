// import java.net.URL

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

description = "A Kotlin multiplatform STOMP client with JVM and JS support"

val coroutinesVersion = "1.3.1"
val jacksonVersion = "2.9.9"
val serializationVersion = "0.14.0"

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
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
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
        ":krossbow-websocket-spring:dokka",
        ":krossbow-websocket-ktor:dokka"
    )
    //    outputFormat = "javadoc"
    multiplatform {
        val global by creating {
            // externalDocumentationLink {
            //     url = URL("file://${project(":krossbow-engine-api").buildDir}/dokka/krossbow-engine-api/")
            //     packageListUrl = URL(url, "package-list")
            // }
        }
        val jvm by creating {
            // externalDocumentationLink {
            //     url = URL("file://${project(":krossbow-engine-spring").buildDir}/dokka/krossbow-engine-spring/")
            //     packageListUrl = URL(url, "package-list")
            // }
        }
        val js by creating {
            // externalDocumentationLink {
            //    url = URL("file://${project(":krossbow-engine-webstompjs").buildDir}/dokka/krossbow-engine-webstompjs/")
            //    packageListUrl = URL(url, "package-list")
            // }
        }
    }
}
