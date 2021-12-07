plugins {
    kotlin("multiplatform")
}

description = "Multiplatform SockJS implementation of Krossbow's WebSocket API."

kotlin {
    jvm()
    js {
        useCommonJs() // required for SockJS top-level declarations usage
        nodejs()
        browser()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.krossbowWebsocketCore)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(projects.krossbowWebsocketSpring)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(npm("sockjs-client", libs.versions.npm.sockjs.client.get()))
            }
        }
    }
}

// suppressing Dokka generation for JS because of the ZipException on NPM dependencies
// https://github.com/Kotlin/dokka/issues/537
tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaLeafTask> {
    dokkaSourceSets.findByName("jsMain")?.suppress?.set(true)
}
