import java.net.URL

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
                implementation(kotlin("stdlib-common"))
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
                implementation(kotlin("stdlib-jdk8"))
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
                implementation(kotlin("stdlib-js"))
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
                url = URL("file://${project(":krossbow-websocket-core").buildDir}/dokka/krossbow-websocket-core/")
                packageListUrl = URL(url, "package-list")
            }
        }
        val jvm by creating {
            externalDocumentationLink {
                url = URL("file://${project(":krossbow-websocket-spring").buildDir}/dokka/krossbow-websocket-spring/")
                packageListUrl = URL(url, "package-list")
            }
        }
        // Dokka disabled for JS because of NPM dependency breaking the generation
        // https://github.com/Kotlin/dokka/issues/537
        // val js by creating {}
    }
}
