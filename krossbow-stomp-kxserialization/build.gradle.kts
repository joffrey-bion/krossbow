plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "An extension of Krossbow STOMP client using Kotlinx Serialization for message conversions"

val serializationVersion = "1.0.0-RC"

kotlin {
    jvm()
    js {
        nodejs()
        browser()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":krossbow-stomp-core"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }
    }
}

tasks.dokka {
    dependsOn(":krossbow-stomp-core:dokka")
    multiplatform {
        val global by creating {
            externalDocumentationLink {
                url = relativeDokkaUrl("krossbow-stomp-core")
                packageListUrl = relativeDokkaPackageListUrl("krossbow-stomp-core")
            }
        }
        val jvm by creating {}
        val js by creating {}
    }
}
