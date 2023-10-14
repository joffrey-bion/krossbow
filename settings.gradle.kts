import de.fayard.refreshVersions.core.DependencySelection
import de.fayard.refreshVersions.core.StabilityLevel

plugins {
    id("com.gradle.enterprise") version "3.15.1"
    id("de.fayard.refreshVersions") version "0.60.3"
}

rootProject.name = "krossbow"

include("krossbow-stomp-core")
include("krossbow-stomp-kxserialization")
include("krossbow-stomp-kxserialization-json")
include("krossbow-stomp-jackson")
include("krossbow-stomp-moshi")
include("krossbow-websocket-core")
include("krossbow-websocket-builtin")
include("krossbow-websocket-ktor")
include("krossbow-websocket-okhttp")
include("krossbow-websocket-sockjs")
include("krossbow-websocket-spring")
include("krossbow-websocket-test")
include("autobahn-tests")
include("autobahn-test-suite")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlways()
    }
}

refreshVersions {
    // workaround for https://github.com/Splitties/refreshVersions/issues/640
    versionsPropertiesFile = file("build/tmp/refreshVersions/versions.properties").apply { parentFile.mkdirs() }

    rejectVersionIf {
        candidate.stabilityLevel != StabilityLevel.Stable || isTyrus() || isOkHttpAlpha()
    }
}

fun DependencySelection.isTyrus() = moduleId.name == "tyrus-standalone-client-jdk"

fun DependencySelection.isOkHttpAlpha() = moduleId.name == "okhttp" && "-alpha" in candidate.value
