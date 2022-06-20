plugins {
    kotlin("multiplatform")
    `kotlin-maven-central-publish`
}

description = "Multiplatform implementation of Krossbow's WebSocket API using Ktor's web sockets."

kotlin {
    jvm()
    jsWithBigTimeouts()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
                api(libs.ktor1.client.websockets)
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
                implementation(libs.ktor1.client.java)
                implementation(libs.ktor1.client.okhttp)
                implementation(libs.slf4j.simple) // avoid warning with java client
            }
        }
    }
}

dokkaExternalDocLink(
    docsUrl = "https://api.ktor.io/older/${libs.versions.ktor1.get()}/ktor-client/",
    packageListUrl = "https://api.ktor.io/older/${libs.versions.ktor1.get()}/package-list",
)
