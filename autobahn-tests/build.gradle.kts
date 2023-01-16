import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.avast.gradle.docker-compose") version "0.16.4"
}

description = "A non-published project to run Autobahn Test Suite on all implementations."

apply<org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolverPlugin>()

kotlin {
    jvm("jvmBuiltin")
    jvm("jvmKtor")
    jvm("jvmOkhttp")
    jvm("jvmSpring")
    jsWithBigTimeouts("jsBuiltin")
    jsWithBigTimeouts("jsKtor")

    nativeTargets("Ktor")
    darwinTargets("Builtin")

    setupMingwLibcurlFor(targetName = "mingwX64Ktor", project)

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(projects.krossbowWebsocketTest)
                implementation(projects.krossbowWebsocketCore)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val jvmTest by creating {
            dependsOn(commonTest)
        }

        val jvmBuiltinTest by getting {
            dependsOn(jvmTest)
            dependencies {
                implementation(projects.krossbowWebsocketBuiltin)
            }
        }

        val jvmOkhttpTest by getting {
            dependsOn(jvmTest)
            dependencies {
                implementation(projects.krossbowWebsocketOkhttp)
            }
        }
        val jvmSpringTest by getting {
            dependsOn(jvmTest)
            dependencies {
                implementation(projects.krossbowWebsocketSpring)
                implementation(libs.jettyWebsocketCient)
                implementation(libs.slf4j.simple) // for jetty client logs
            }
        }
        val jvmKtorTest by getting {
            dependsOn(jvmTest)
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
                implementation(libs.ktor.client.java)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.slf4j.simple)
            }
        }

        val jsTest by creating {
            dependsOn(commonTest)
            dependencies {
                // to call the Autobahn HTTP APIs
                implementation(npm("isomorphic-fetch", libs.versions.npm.isomorphic.fetch.get()))
            }
        }
        val jsBuiltinTest by getting {
            dependsOn(jsTest)
            dependencies {
                implementation(projects.krossbowWebsocketBuiltin)
                implementation(npm("isomorphic-ws", libs.versions.npm.isomorphic.ws.get()))
                implementation(npm("ws", libs.versions.npm.ws.get()))
            }
        }
        val jsKtorTest by getting {
            dependsOn(jsTest)
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
                implementation(libs.ktor.client.js)
            }
        }

        // Those common native source sets are for the HTTP getter actual implementations.
        // This is used during tests to access Autobahn reports via HTTP.
        val nativeTest by creating {
            dependsOn(commonTest)
            dependencies {
                implementation(libs.ktor.client.core)
            }
        }
        val darwinTest by creating {
            dependsOn(nativeTest)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val linuxX64Test by creating {
            dependsOn(nativeTest)
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        val mingwX64Test by creating {
            dependsOn(nativeTest)
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }

        val unixKtorTest by getting {
            dependsOn(nativeTest)
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
                implementation(libs.ktor.client.cio)
            }
        }
        val linuxX64KtorTest by getting {
            dependsOn(linuxX64Test)
        }
        val darwinKtorTest by getting {
            dependsOn(darwinTest)
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
            }
        }
        val mingwX64KtorTest by getting {
            dependsOn(mingwX64Test)
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
                implementation(libs.ktor.client.curl)
            }
        }

        val darwinBuiltinTest by getting {
            dependsOn(darwinTest)
            dependencies {
                implementation(projects.krossbowWebsocketBuiltin)
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
    // autobahn doesn't support parallel tests (/getCaseStatus fails with immediate Close frame)
    // https://github.com/crossbario/autobahn-testsuite/issues/119
    maxParallelForks = 1

    doFirst {
        val autobahnContainer = getAutobahnTestServerContainerInfo()
        environment("AUTOBAHN_SERVER_HOST", autobahnContainer.host)
        environment("AUTOBAHN_SERVER_TCP_8080", autobahnContainer.ports.getValue(8080))
        environment("AUTOBAHN_SERVER_TCP_9001", autobahnContainer.ports.getValue(9001))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    doFirst {
        val autobahnContainer = getAutobahnTestServerContainerInfo()
        environment("AUTOBAHN_SERVER_HOST", autobahnContainer.host)
        environment("AUTOBAHN_SERVER_TCP_8080", autobahnContainer.ports.getValue(8080))
        environment("AUTOBAHN_SERVER_TCP_9001", autobahnContainer.ports.getValue(9001))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest> {
    doFirst {
        val autobahnContainer = getAutobahnTestServerContainerInfo()
        // SIMCTL_CHILD_ prefix to pass those variables from test process to the iOS/tvOS/watchOS emulators
        environment("SIMCTL_CHILD_AUTOBAHN_SERVER_HOST", autobahnContainer.host)
        environment("SIMCTL_CHILD_AUTOBAHN_SERVER_TCP_8080", autobahnContainer.ports.getValue(8080))
        environment("SIMCTL_CHILD_AUTOBAHN_SERVER_TCP_9001", autobahnContainer.ports.getValue(9001))
    }
}

val jsTestTasks = tasks.withType<org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest>().map { it }

jsTestTasks.forEach { testTask ->
    val npmProjectDir = testTask.compilation.npmProject.dir
    val autobahnServerConfigFile = npmProjectDir.resolve("autobahn-server.json")

    val taskPrefix = npmProjectDir.name.removePrefix("krossbow-autobahn-tests").toCamelCase()
    val generateConfigTaskName = "${taskPrefix}_autobahnConfig"

    // There is a browser test task AND a node test task for each source set, and they use the same NPM project dir.
    // There is a legacy AND an IR test task for each source set, but they DON'T use the same NPM project dir.
    // We only need to create the task once per generated NPM project, but we need all test tasks using this project to
    // depend on the config generation task.
    val generateAutobahnConfigJson = tasks.findByName(generateConfigTaskName) ?: tasks.create(generateConfigTaskName) {
        group = "autobahn config js"
        dockerCompose.isRequiredBy(this)
        outputs.file(autobahnServerConfigFile)
        doFirst {
            val autobahnContainer = getAutobahnTestServerContainerInfo()
            file(autobahnServerConfigFile).writeText(
                """{
                    "host":"${autobahnContainer.host}",
                    "webPort":${autobahnContainer.ports.getValue(8080)},
                    "wsPort":${autobahnContainer.ports.getValue(9001)}
                }""".trimMargin()
            )
        }
    }

    testTask.dependsOn(generateAutobahnConfigJson)
}

fun getAutobahnTestServerContainerInfo() = dockerCompose.servicesInfos["autobahn_server"]?.firstContainer
    ?: error("autobahn_server container not found")

fun String.toCamelCase() = replace(Regex("""\-(\w)""")) { match -> match.groupValues[1].toUpperCase() }
