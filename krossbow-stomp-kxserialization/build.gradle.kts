plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

description = "An extension of Krossbow STOMP client using Kotlinx Serialization for message conversions"

val serializationVersion = "1.1.0"

kotlin {
    jvm()
    js {
        nodejs()
        browser()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowStompCore)
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }
    }
}
