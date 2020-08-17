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
                api(project(":krossbow-websocket-core"))
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
                api(project(":krossbow-websocket-spring"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(npm("sockjs-client", "1.4.0"))
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
        ":krossbow-websocket-core:dokka",
        ":krossbow-websocket-spring:dokka"
    )
    multiplatform {
        val global by creating {
            externalDocumentationLink {
                url = relativeDokkaUrl("krossbow-websocket-core")
                packageListUrl = relativeDokkaPackageListUrl("krossbow-websocket-core")
            }
        }
        val jvm by creating {
            externalDocumentationLink {
                url = relativeDokkaUrl("krossbow-websocket-spring")
                packageListUrl = relativeDokkaPackageListUrl("krossbow-websocket-spring")
            }
        }
        // Dokka disabled for JS because of NPM dependency breaking the generation
        // https://github.com/Kotlin/dokka/issues/537
        // val js by creating {}
    }
}
