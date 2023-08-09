plugins {
    id("krossbow-multiplatform-all")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(projects.krossbowWebsocketTest)
                implementation(projects.krossbowWebsocketCore)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val jvmMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                // to call the Autobahn HTTP APIs
                implementation(npm("isomorphic-fetch", libs.versions.npm.isomorphic.fetch.get()))
            }
        }

        // Those native source sets are for the HTTP getter actual implementations.
        // This is used during tests to access Autobahn reports via HTTP.
        val nativeMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.core)
            }
        }
        val darwinMain by getting {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val linuxX64Main by getting {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        val mingwX64Main by getting {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
    }
}
