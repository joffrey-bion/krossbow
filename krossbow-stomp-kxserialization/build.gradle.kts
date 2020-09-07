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
