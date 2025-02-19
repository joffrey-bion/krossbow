plugins {
    id("com.gradle.develocity") version "3.19.2"
}

rootProject.name = "krossbow"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

includeBuild("gradle/plugins")
includeBuild("test-server")
include("krossbow-io")
include("krossbow-stomp-core")
include("krossbow-stomp-kxserialization")
include("krossbow-stomp-kxserialization-json")
include("krossbow-stomp-jackson")
include("krossbow-stomp-moshi")
include("krossbow-websocket-core")
include("krossbow-websocket-builtin")
include("krossbow-websocket-ktor")
include("krossbow-websocket-ktor-legacy")
include("krossbow-websocket-okhttp")
include("krossbow-websocket-sockjs")
include("krossbow-websocket-spring")
include("krossbow-websocket-test")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        uploadInBackground = false // background upload is bad for CI, and not critical for local runs
    }
}
