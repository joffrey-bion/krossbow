plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.avast.gradle.docker-compose") version "0.14.9"
}

description = "A non-published project to run Autobahn Test Suite on all implementations."

kotlin {
    jvm()
    js {
        useCommonJs()
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
        browser {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
    }
    ios()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(projects.krossbowWebsocketTest)
                implementation(projects.krossbowWebsocketCore)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
                implementation(projects.krossbowWebsocketOkhttp)
                implementation(projects.krossbowWebsocketSpring)
                implementation(libs.ktor.client.java)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.jettyWebsocketCient)
                implementation(libs.slf4j.simple) // for jetty client logs
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
                implementation(libs.ktor.client.js)
                implementation(npm("isomorphic-ws", libs.versions.npm.isomorphic.ws.get()))
                implementation(npm("ws", libs.versions.npm.ws.get()))
            }
        }
    }
}

// autobahn test server for websocket tests
dockerCompose {
    useComposeFiles.set(listOf(file("$projectDir/test-server/docker-compose.yml").toString()))
    buildBeforeUp.set(false)
}

// ensure autobahn test server is launched for websocket tests
tasks.withType<AbstractTestTask> {
    dockerCompose.isRequiredBy(this)
}

// provide autobahn test server coordinates to the tests (can vary if DOCKER_HOST is set - like on CI macOS)
tasks.withType<org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest> {
    doFirst {
        val autobahnContainer = getAutobahnTestServerContainerInfo()
        environment("AUTOBAHN_SERVER_HOST", autobahnContainer.host)
        environment("AUTOBAHN_SERVER_TCP_9001", autobahnContainer.port)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest> {
    doFirst {
        val autobahnContainer = getAutobahnTestServerContainerInfo()
        // SIMCTL_CHILD_ prefix to pass those variables from test process to the iOS emulator
        environment("SIMCTL_CHILD_AUTOBAHN_SERVER_HOST", autobahnContainer.host)
        environment("SIMCTL_CHILD_AUTOBAHN_SERVER_TCP_9001", autobahnContainer.port)
    }
}

val generateAutobahnConfigJsonForJs by tasks.creating {
    dockerCompose.isRequiredBy(this)
    val config = "${rootProject.buildDir}/js/packages/${rootProject.name}-${project.name}-test/autobahn-server.json"
    outputs.file(config)
    doFirst {
        val autobahnContainer = getAutobahnTestServerContainerInfo()
        file(config).writeText("""{"host":"${autobahnContainer.host}","port":${autobahnContainer.port}}""")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest> {
    dependsOn(generateAutobahnConfigJsonForJs)
}

fun getAutobahnTestServerContainerInfo() = dockerCompose.servicesInfos["autobahn_server"]?.firstContainer
    ?: error("autobahn_server container not found")
