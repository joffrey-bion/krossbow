plugins {
    id("krossbow-multiplatform-all")
    id("krossbow-publish")
}

description = "A Kotlin multiplatform STOMP client based on a generic web socket API"

kotlin {
    sourceSets {
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
                implementation(libs.slf4j.test)
            }
        }
    }
}
