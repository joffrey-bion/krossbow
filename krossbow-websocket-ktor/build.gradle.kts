plugins {
    kotlin("multiplatform")
    `kotlin-maven-central-publish`
}

description = "Multiplatform implementation of Krossbow's WebSocket API using Ktor's web sockets."

kotlin {
    jvm()
    jsWithBigTimeouts()
    setupNativeTargets()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.ktor2.client.websockets)
                api(libs.kotlinx.atomicfu)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(projects.krossbowWebsocketTest)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.ktor2.client.java)
                implementation(libs.ktor2.client.okhttp)
                implementation(libs.slf4j.simple)
            }
        }

        setupNativeSourceSets()

        val nativeDarwinTest by getting {
            dependencies {
                implementation(libs.ktor2.client.darwin)
            }
        }
    }
}

dokkaExternalDocLink(
    docsUrl = "https://api.ktor.io/ktor-client/",
    packageListUrl = "https://api.ktor.io/package-list",
)
