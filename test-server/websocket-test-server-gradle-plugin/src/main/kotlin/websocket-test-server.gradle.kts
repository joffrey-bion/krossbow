import org.hildan.krossbow.test.server.*
import org.hildan.krossbow.test.server.TestServer

abstract class WSTestServerService : BuildService<BuildServiceParameters.None>, AutoCloseable {

    val wsServer: TestServer = startTestServer()

    override fun close() {
        wsServer.stop()
    }
}

// TODO remove empty trailing lambda in Gradle 8.7
val wsTestServerService = gradle.sharedServices.registerIfAbsent("wsTestServer", WSTestServerService::class) {}

// ensure the test server is launched for websocket tests
tasks.withType<AbstractTestTask> {
    usesService(wsTestServerService)
}

// The Test task is a parent of all JVM test tasks, including KMP/JVM and pure JVM.
// This is why we need to use this instead of KotlinJvmTest.
tasks.withType<Test> {
    doFirst {
        val testServer = wsTestServerService.get().wsServer
        environment("TEST_SERVER_HOST", testServer.host)
        environment("TEST_SERVER_HTTP_PORT", testServer.httpPort)
        environment("TEST_SERVER_WS_PORT", testServer.wsPort)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    doFirst {
        val testServer = wsTestServerService.get().wsServer
        environment("TEST_SERVER_HOST", testServer.host)
        environment("TEST_SERVER_HTTP_PORT", testServer.httpPort)
        environment("TEST_SERVER_WS_PORT", testServer.wsPort)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest> {
    doFirst {
        val testServer = wsTestServerService.get().wsServer
        // SIMCTL_CHILD_ prefix to pass those variables from test process to the iOS/tvOS/watchOS emulators
        environment("SIMCTL_CHILD_TEST_SERVER_HOST", testServer.host)
        environment("SIMCTL_CHILD_TEST_SERVER_HTTP_PORT", testServer.httpPort)
        environment("SIMCTL_CHILD_TEST_SERVER_WS_PORT", testServer.wsPort)
    }
}

// In NodeJS tests, these environment variables can be read directly from tests.
// In browser (Karma) tests, the environment variables are passed to Karma, and then we must configure Karma to pass
// this data on to the code under test (see generateKarmaConfig task below).
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest> {
    dependsOn(generateKarmaConfig)
    doFirst {
        val testServer = wsTestServerService.get().wsServer
        environment("TEST_SERVER_HOST", testServer.host)
        environment("TEST_SERVER_HTTP_PORT", testServer.httpPort.toString())
        environment("TEST_SERVER_WS_PORT", testServer.wsPort.toString())
    }
}

val generateKarmaConfig by tasks.registering {
    group = "js test setup"
    description = "Generates a Karma configuration that exposes the 'testServerConfig' variable to the browser, " +
        "which contains the host and ports of the running test server."

    val karmaConfigFile = layout.projectDirectory.file("karma.config.d/expose-test-server-config.js")
    outputs.file(karmaConfigFile)

    doFirst {
        // language=javascript
        karmaConfigFile.asFile.writeText("""
            const webpack = require('webpack');
            
            // To increase the internal mocha test timeout (cannot be done from DSL)
            // https://youtrack.jetbrains.com/issue/KT-56718#focus=Comments-27-6905607.0-0
            config.set({
                client: {
                    mocha: {
                        // Some tests with multiple calls exceed 10s (e.g. tests of status codes ranges).
                        // We put a large timeout here so we can adjust it in the tests themselves.
                        timeout: 60000
                    }
                }
            });
            
            // This is to transfer the test server config from the Karma process to the browser environment.
            // It defines the gobal variable 'testServerConfig', which can then be accessed from the code under test.
            config.webpack.plugins.push(
                new webpack.DefinePlugin({
                    "testServerConfig" : {
                        "host": '"' + process.env.TEST_SERVER_HOST + '"',
                        "httpPort": process.env.TEST_SERVER_HTTP_PORT,
                        "wsPort": process.env.TEST_SERVER_WS_PORT
                    }
                })
            )
        """.trimIndent())
    }
}
