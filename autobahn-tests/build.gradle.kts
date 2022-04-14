import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.avast.gradle.docker-compose") version "0.14.9"
}

description = "A non-published project to run Autobahn Test Suite on all implementations."

apply<org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolverPlugin>()

kotlin {
    jvm()
    jvm("jvmKtor")
    jvm("jvmKtorLegacy")
    jvm("jvmOkhttp")
    jvm("jvmSpring")
    jsWithBigTimeouts("js")
    jsWithBigTimeouts("jsKtor")
    jsWithBigTimeouts("jsKtorLegacy")
    jsWithBigTimeouts("jsOther")
    setupNativeTargets()

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
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val jvmTest by getting {}

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
        val jvmKtorLegacyTest by getting {
            dependsOn(jvmTest)
            dependencies {
                implementation(projects.krossbowWebsocketKtorLegacy)
                implementation(libs.ktor1.client.java)
                implementation(libs.ktor1.client.okhttp)
            }
        }
        val jvmKtorTest by getting {
            dependsOn(jvmTest)
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
                implementation(libs.ktor2.client.java)
                implementation(libs.ktor2.client.okhttp)
                implementation(libs.slf4j.simple)
            }
        }

        val jsTest by getting {}

        val jsKtorTest by getting {
            dependsOn(jsTest)
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
                implementation(libs.ktor2.client.js)
            }
        }
        val jsKtorLegacyTest by getting {
            dependsOn(jsTest)
            dependencies {
                implementation(projects.krossbowWebsocketKtorLegacy)
                implementation(libs.ktor1.client.js)
            }
        }
        val jsOtherTest by getting {
            dependsOn(jsTest)
            dependencies {
                // to call the Autobahn HTTP APIs
                implementation(npm("isomorphic-fetch", libs.versions.npm.isomorphic.fetch.get()))

                implementation(npm("isomorphic-ws", libs.versions.npm.isomorphic.ws.get()))
                implementation(npm("ws", libs.versions.npm.ws.get()))
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
    val sourceSet = testTask.compilation.defaultSourceSetName
    val npmProjectDir = testTask.compilation.npmProject.dir
    val autobahnServerConfigFile = npmProjectDir.resolve("autobahn-server.json")

    val generateConfigTaskName = "${sourceSet}_autobahnConfig"

    // There is a browser test task AND a node test task for each source set, and they use the same NPM project dir.
    // So we only need to create the task once per source set, but we need both test tasks to depend on it.
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
