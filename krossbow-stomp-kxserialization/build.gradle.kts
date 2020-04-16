import java.net.URL

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

description = "An extension of Krossbow STOMP client using Kotlinx Serialization for message conversions"

val serializationVersion = "0.20.0"

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
                api(project(":krossbow-stomp-core"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
            }
        }
        val jvmMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
            }
        }
        val jsMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
            }
        }
    }
}

tasks.dokka {
    dependsOn(":krossbow-stomp-core:dokka")
    multiplatform {
        val global by creating {
            externalDocumentationLink {
                url = URL("file://${project(":krossbow-stomp-core").buildDir}/dokka/krossbow-stomp-core/")
                packageListUrl = URL(url, "package-list")
            }
        }
        val jvm by creating {}
        val js by creating {}
    }
}
