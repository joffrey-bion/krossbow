import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.avast.docker.compose)
}

description = "A non-published project to run Autobahn Test Suite on all implementations."

apply<org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolverPlugin>()

val websocketEngineAttribute = Attribute.of("websocket-engine", String::class.java)

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        group("unixKtor") {
            withLinux()
            group("appleKtor") {
                withApple()
            }
        }
        group("appleBuiltin") {
            withApple()
        }
    }
    jvm("jvmBuiltin") {
        attributes.attribute(websocketEngineAttribute, "builtin-jvm")
    }
    jvm("jvmKtor") {
        attributes.attribute(websocketEngineAttribute, "ktor")
    }
    jvm("jvmOkhttp") {
        attributes.attribute(websocketEngineAttribute, "okhttp")
    }
    jvm("jvmSpring") {
        attributes.attribute(websocketEngineAttribute, "spring")
    }
    jsWithBigTimeouts("jsBuiltin") {
        attributes.attribute(websocketEngineAttribute, "builtin-js")
    }
    jsWithBigTimeouts("jsKtor") {
        attributes.attribute(websocketEngineAttribute, "ktor")
    }
    nativeTargets("Ktor") {
        attributes.attribute(websocketEngineAttribute, "ktor")
    }
    appleTargets("Builtin") {
        attributes.attribute(websocketEngineAttribute, "builtin-darwin")
    }

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(projects.autobahnTestSuite)
                implementation(projects.krossbowWebsocketTest)
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

        val jsBuiltinTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(projects.krossbowWebsocketBuiltin)
                implementation(npm("isomorphic-ws", libs.versions.npm.isomorphic.ws.get()))
                implementation(npm("ws", libs.versions.npm.ws.get()))
            }
        }
        val jsKtorTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
                implementation(libs.ktor.client.js)
            }
        }

        val unixKtorTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
                implementation(libs.ktor.client.cio)
            }
        }
        val appleKtorTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
                implementation(libs.ktor.client.darwin)
            }
        }
        val mingwX64KtorTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketKtor)
                implementation(libs.ktor.client.winhttp)
            }
        }

        val appleBuiltinTest by getting {
            dependencies {
                implementation(projects.krossbowWebsocketBuiltin)
            }
        }
    }
}

// workaround for KT-55751 / KT-56450, required in Gradle 8
// https://youtrack.jetbrains.com/issue/KT-55751/MPP-Gradle-Consumable-configurations-must-have-unique-attributes
// https://youtrack.jetbrains.com/issue/KT-56450/Custom-attributes-declared-for-target-arent-included-in-metadata-variant
configurations.configureEach {
    if (name.startsWith("metadata")) {
        return@configureEach
    }
    if (name.endsWith("ApiElements") || name.endsWith("RuntimeElements") || name.endsWith("SourcesElements")) {
        val targetName = name.removeSuffix("ApiElements")
            .removeSuffix("RuntimeElements")
            .removeSuffix("SourcesElements")
            .removeSuffix("CInterop")
            .replace("Ir", "Legacy")
        val target = kotlin.targets.getByName(targetName)
        val engine = target.attributes.getAttribute(websocketEngineAttribute)
            ?: error("engine attribute not found in target $targetName (for configuration $name)")
        attributes {
            attribute(websocketEngineAttribute, engine)
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
